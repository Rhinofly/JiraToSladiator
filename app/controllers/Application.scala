package controllers

import java.text.SimpleDateFormat
import play.api.libs.json.Json.toJson
import play.api.libs.json.JsValue
import play.api.libs.ws.WS
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.Logger
import play.api.libs.json.JsArray
import com.ning.http.client.Realm
import play.api.http.ContentTypes
import play.api.libs.json.JsString
import play.api.Play

object Application extends Controller {

  def index = Action { request =>
    Ok(views.html.index(request.host))
  }

  // from 2012-12-14T15:59:09.145+0100 to 2013-01-01 13:13:13 +05:00
  def toSladiatorDate(date: JsValue): JsValue =
    toJson(
      date.asOpt[String]
        .filter(!_.isEmpty)
        .map(date => date replaceAll ("""(\d{4}-\d{2}-\d{2})T(\d{2}:\d{2}:\d{2})\.\d{3}([+-]\d{4})""", "$1 $2 $3")))

  case class Transition(date: JsValue, from: Option[String], to: String)

  def normalizeTransitions(json: JsValue) = {
    val issueFields = json \ "fields"

    val history = (json \ "changelog" \ "histories").as[Seq[JsValue]]

    val knownTransitions = history flatMap { historyItem =>

      val transition = (historyItem \ "items")
        .as[Seq[JsValue]]
        .filter(item => (item \ "field").as[String] == "status")
        .headOption

      transition map { item =>
        Transition(historyItem \ "created", Some((item \ "fromString").as[String]), (item \ "toString").as[String])
      }
    }

    val firstTransition = Transition(
      issueFields \ "created",
      None,
      // use the from of the first known transition or else the current status of the issue
      knownTransitions.headOption flatMap (_.from) getOrElse (issueFields \ "status" \ "name").as[String])

    firstTransition +: knownTransitions
  }

  def convertTransitions(transitions: Seq[Transition]) = {
    val sladiatorTransitions =
      (transitions foldLeft Vector[Map[String, JsValue]]()) { (transitions, transition) =>

        val previousSladiatorTransition =
          transitions.lastOption map { previousTransition =>
            previousTransition
              .updated("status_to", JsString(transition.to))
              .updated("exited_at", toSladiatorDate(transition.date))
          }

        val newSladiatorTransition = Map(
          "status" -> JsString(transition.to),
          "entered_at" -> toSladiatorDate(transition.date))

        // replace the previous transition
        val newTransitions =
          previousSladiatorTransition map { previousTransition =>
            (transitions dropRight 1) :+ previousTransition
          } getOrElse transitions

        newTransitions :+ newSladiatorTransition
      }

    toJson(sladiatorTransitions.toArray)
  }

  def convert(json: JsValue, host:String) = {

    val jiraIssue = json

    val key = jiraIssue \ "key"

    val fields = jiraIssue \ "fields"
    val updatedAt = toSladiatorDate(fields \ "updated")

    val transitions = convertTransitions(normalizeTransitions(json))

    toJson(Map(
      ("key", key),
      ("url", toJson("https://" + host + "/browse/" + key.as[String])),
      ("issue_type", fields \ "issuetype" \ "name"),
      ("status", fields \ "status" \ "name"),
      ("priority", fields \ "priority" \ "name"),
      ("project", fields \ "project" \ "key"),
      ("issue_created_at", toSladiatorDate(fields \ "created")),
      ("issue_updated_at", toSladiatorDate(fields \ "updated")),
      ("assignee", fields \ "assignee" \ "name"),
      ("assignee_avatar_url", fields \ "assignee" \ "avatarUrls" \ "48x48"),
      ("assignee_email", fields \ "assignee" \ "emailAddress"),
      ("resolution", fields \ "resolution" \ "name"),
      ("resolution_date", toSladiatorDate(fields \ "resolutiondate")),
      ("due_date", fields \ "duedate"),
      ("components", toJson(fields \ "components" \\ "name")),
      ("labels", fields \ "labels"),
      ("labels", fields \ "labels"),
      ("source", toJson("Jira webhook -> Sladiator connector by Rhinofly")),
      ("transitions", transitions)))
  }

  def getIssueInformation(issueKey: String, host:String, jiraUsername: String, jiraPassword: String) =
    WS
      .url("https://%s/rest/api/2/issue/%s?fields=" +
        "issuetype,status,priority,project,created,updated," +
        "assignee,resolution,resolutiondate,duedate,components,labels" +
        "&expand=changelog" format (host, issueKey))
      .withAuth(jiraUsername, jiraPassword, Realm.AuthScheme.BASIC)
      .get

  def testJiraApi(issueKey: String, host:String, jiraUsername: String, jiraPassword: String) = Action {
    Async {
      getIssueInformation(issueKey, host, jiraUsername, jiraPassword).map { response =>
        Ok(response.body).as(ContentTypes.HTML)
      }
    }
  }

  def webhook(sladiatorToken: String, issueKey: String, host:String, jiraUsername: String, jiraPassword: String) = Action {

    Logger.info("webhook called for host: " + host)

    val issueInformation = getIssueInformation(issueKey, host, jiraUsername, jiraPassword)

    issueInformation onRedeem { response =>
      response.status match {

        // call the sladiator web service
        case 200 => {

          val sladiatorIssue = convert(response.json, host)

          val result = WS
            .url("https://sladiator.com/api/tickets")
            .withHeaders("SLA_TOKEN" -> sladiatorToken)
            .post(sladiatorIssue)

          result onRedeem { response =>
            val status = (response.json \ "status").asOpt[String]

            status match {
              case Some(value) if (value == "Ok") => // no problem
              case status => Logger error status.toString
            }
          }
        }
        case status => Logger error ("Error, got status %s from jira for sladiator token '%s' and issueKey '%s'" format (status, sladiatorToken, issueKey))
      }
    }

    Ok
  }
}
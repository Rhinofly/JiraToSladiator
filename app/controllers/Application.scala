package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json.Json._
import play.api.libs.ws.WS
import java.text.SimpleDateFormat
import play.api.libs.json.JsValue

object Application extends Controller {

  //2012-12-14T15:59:09.145+0100
  val jiraDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
  //"2012-12-13 12:15:05"
  val sladiatorDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

  def toSladiatorDate(date: JsValue): JsValue =
    toJson(
      date.asOpt[String]
        .map(date => sladiatorDateFormat format (jiraDateFormat parse date)))

  def index = Action { request =>
    //Logger info (request.body.asJson map (_.toString)).getOrElse("no data")

    request.body.asJson foreach { json =>
      val jiraIssue = json \ "issue"

      val hostRegex = """(.+?://.+?/).*""".r
      val hostRegex(host) = (jiraIssue \ "self").as[String]

      val key = jiraIssue \ "key"

      val fields = jiraIssue \ "fields"

      val sladiatorIssue = toJson(Map(
        ("key", key),
        ("url", toJson(host + "browse/" + key.as[String])),
        ("issue_type", fields \ "issuetype" \ "name"),
        ("status", fields \ "status" \ "name"),
        ("priority", fields \ "priority" \ "name"),
        ("project", fields \ "project" \ "key"),
        ("issue_created_at", toSladiatorDate(fields \ "created")),
        ("issue_updated_at", toSladiatorDate(fields \ "updated")),
        ("assignee", fields \ "assignee" \ "name"),
        ("assignee_avatar_url", fields \ "assignee" \ "avatarUrls" \ "48x48"),
        ("assignee_email", fields \ "assignee" \ "emailAddress"),
        ("resolution", fields \ "resolution"),
        ("resolution_date", toSladiatorDate(fields \ "resolutiondate")),
        ("due_date", toSladiatorDate(fields \ "duedate")),
        ("components", toJson(fields \ "components" \\ "name")),
        ("labels", fields \ "labels"),
        ("labels", fields \ "labels"),
        ("source", toJson("Jira webhook -> Sladiator connector by Rhinofly"))))

      val result = WS
        .url("https://sladiator.com/api/tickets")
        .withHeaders("SLA_TOKEN" -> "4bfff60a464d23b52ad715cc1fc059be8cc1a9eb")
        .post(sladiatorIssue)

      result onRedeem { response =>
        val status = (response.json \ "status").asOpt[String]

        status match {
          case Some(value) if (value == "Ok") => // no problem
          case status => Logger error status.toString
        }
      }
    }

    Ok
  }
}
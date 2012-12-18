package controllers

import java.text.SimpleDateFormat

import play.api.libs.json.Json.toJson
import play.api.libs.json.JsValue
import play.api.libs.ws.WS
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.Logger

object Application extends Controller {

  def index = Action { request =>
    Ok(views.html.index(request.host))
  }

  val dueDate = new SimpleDateFormat("yyyy-MM-dd")
  //2012-12-14T15:59:09.145+0100
  implicit val jiraDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
  //2012-12-13 12:15:05
  val sladiatorDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

  def toSladiatorDate(date: JsValue)(implicit source:SimpleDateFormat): JsValue =
    toJson(
      date.asOpt[String]
        .map(date => sladiatorDateFormat format (source parse date)))

  def webhook(token:String) = Action { request =>

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
        ("due_date", toSladiatorDate(fields \ "duedate")(dueDate)),
        ("components", toJson(fields \ "components" \\ "name")),
        ("labels", fields \ "labels"),
        ("labels", fields \ "labels"),
        ("source", toJson("Jira webhook -> Sladiator connector by Rhinofly"))))

      val result = WS
        .url("https://sladiator.com/api/tickets")
        .withHeaders("SLA_TOKEN" -> token)
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
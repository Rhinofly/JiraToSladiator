package controllers

import org.specs2.Specification
import play.api.libs.json.JsValue
import play.api.libs.json.JsString
import play.api.libs.json.JsUndefined
import play.api.libs.json.JsNull
import play.api.libs.json.Json
import com.lunatech.jsoncompare.JsonCompare.{ caseInsensitiveSubTree => c }

object ApplicationSpec extends Specification {
  import Application._

  def is =
    "Jira to Sladiator" ^
      "convert should" ^
      "be able to convert simple jira json to sladiator json" ! testConvert ^
      "be able to convert jira json with change of status to sladiator json" ! testConvertStatusTransition ^
      "toSladiatorDate should " ^
      "be able to handle empty values" ! testEmptyValue ^
      "convert the date/time to a date/time that includes the timezone offset" ! testToSladiatorDate ^
      end

  def testConvert = {
    val jira = Json parse
      """{
    "expand": "renderedFields,names,schema,transitions,operations,editmeta,changelog",
    "id": "10949",
    "self": "https://rhinofly.atlassian.net/rest/api/2/issue/10949",
    "key": "CSOI-12",
    "fields": {
        "project": {
            "self": "https://rhinofly.atlassian.net/rest/api/2/project/CSOI",
            "id": "10403",
            "key": "CSOI",
            "name": "CSO Incidenten",
            "avatarUrls": {
                "16x16": "https://rhinofly.atlassian.net/secure/projectavatar?size=small&pid=10403&avatarId=10011",
                "48x48": "https://rhinofly.atlassian.net/secure/projectavatar?pid=10403&avatarId=10011"
            }
        },
    	"issuetype": {
            "self": "https://rhinofly.atlassian.net/rest/api/2/issuetype/1",
            "id": "1",
            "description": "A problem which impairs or prevents the functions of the product.",
            "iconUrl": "https://rhinofly.atlassian.net/images/icons/issuetypes/bug.png",
            "name": "Bug",
            "subtask": false
        },
        "status": {
            "self": "https://rhinofly.atlassian.net/rest/api/2/status/10004",
            "description": "",
            "iconUrl": "https://rhinofly.atlassian.net/images/icons/statuses/unassigned.png",
            "name": "New",
            "id": "10004"
        },
        "updated": "2013-01-09T14:09:40.790+0100",
        "created": "2013-01-09T14:09:40.790+0100",
        "priority": {
            "self": "https://rhinofly.atlassian.net/rest/api/2/priority/3",
            "iconUrl": "https://rhinofly.atlassian.net/images/icons/priorities/major.png",
            "name": "Major",
            "id": "3"
        },
        "labels": [],
        "duedate": null,
        "assignee": {
            "self": "https://rhinofly.atlassian.net/rest/api/2/user?username=dvink",
            "name": "dvink",
            "emailAddress": "dvink@rhinofly.nl",
            "avatarUrls": {
                "16x16": "https://rhinofly.atlassian.net/secure/useravatar?size=small&ownerId=dvink&avatarId=10600",
                "48x48": "https://rhinofly.atlassian.net/secure/useravatar?ownerId=dvink&avatarId=10600"
            },
            "displayName": "Deef Vink",
            "active": true
        },
        "resolution": {
    		"name" : "Fixed"
    	},
        "components": [],
        "resolutiondate": "2013-01-03T16:36:47.012+0100"
    },
    "changelog": {
        "startAt": 0,
        "maxResults": 0,
        "total": 0,
        "histories": []
    }
}"""
    val sladiator = convert(jira, "rhinofly.atlassian.net")

    c(sladiator, Json.parse("""
	{
		"resolution_date":"2013-01-03 16:36:47 +0100",
		"priority":"Major",
		"assignee":"dvink",
		"source":"Jira webhook -> Sladiator connector by Rhinofly",
		"issue_updated_at":"2013-01-09 14:09:40 +0100",
		"url":"https://rhinofly.atlassian.net/browse/CSOI-12",
		"project":"CSOI",
		"issue_type":"Bug",
		"components":[],
		"key":"CSOI-12",
		"labels":[],
		"resolution":"Fixed",
		"assignee_avatar_url":"https://rhinofly.atlassian.net/secure/useravatar?ownerId=dvink&avatarId=10600",
		"status":"New",
		"assignee_email":"dvink@rhinofly.nl",
		"due_date":null,
		"issue_created_at":"2013-01-09 14:09:40 +0100",
        "transitions" : [
    		{
      			"entered_at" : "2013-01-09 14:09:40 +0100",
      			"status" : "New"
    		}]
	}""")) must beRight

  }

  def testConvertStatusTransition = {

    val jira = Json parse
      """{
    "expand": "renderedFields,names,schema,transitions,operations,editmeta,changelog",
    "id": "10943",
    "self": "https://rhinofly.atlassian.net/rest/api/2/issue/10943",
    "key": "CSOI-9",
    "fields": {
        "project": {
            "self": "https://rhinofly.atlassian.net/rest/api/2/project/CSOI",
            "id": "10403",
            "key": "CSOI",
            "name": "CSO Incidenten",
            "avatarUrls": {
                "16x16": "https://rhinofly.atlassian.net/secure/projectavatar?size=small&pid=10403&avatarId=10011",
                "48x48": "https://rhinofly.atlassian.net/secure/projectavatar?pid=10403&avatarId=10011"
            }
        },
    	"issuetype": {
            "self": "https://rhinofly.atlassian.net/rest/api/2/issuetype/1",
            "id": "1",
            "description": "A problem which impairs or prevents the functions of the product.",
            "iconUrl": "https://rhinofly.atlassian.net/images/icons/issuetypes/bug.png",
            "name": "Bug",
            "subtask": false
        },
        "status": {
            "self": "https://rhinofly.atlassian.net/rest/api/2/status/10007",
            "description": "",
            "iconUrl": "https://rhinofly.atlassian.net/images/icons/statuses/information.png",
            "name": "Waiting for Customer",
            "id": "10007"
        },
        "updated": "2013-01-08T17:24:12.687+0100",
        "created": "2013-01-08T14:13:03.420+0100",
        "priority": {
            "self": "https://rhinofly.atlassian.net/rest/api/2/priority/3",
            "iconUrl": "https://rhinofly.atlassian.net/images/icons/priorities/major.png",
            "name": "Major",
            "id": "3"
        },
        "labels": [],
        "duedate": null,
        "assignee": {
            "self": "https://rhinofly.atlassian.net/rest/api/2/user?username=ewestra",
            "name": "ewestra",
            "emailAddress": "ewestra@rhinofly.nl",
            "avatarUrls": {
                "16x16": "https://rhinofly.atlassian.net/secure/useravatar?size=small&ownerId=ewestra&avatarId=10200",
                "48x48": "https://rhinofly.atlassian.net/secure/useravatar?ownerId=ewestra&avatarId=10200"
            },
            "displayName": "Erik Westra [Administrator]",
            "active": true
        },
        "resolution": null,
        "components": [],
        "resolutiondate": null
    },
    "changelog": {
        "startAt": 0,
        "maxResults": 18,
        "total": 18,
        "histories": [{
            "id": "13101",
            "author": {
                "self": "https://rhinofly.atlassian.net/rest/api/2/user?username=ewestra",
                "name": "ewestra",
                "emailAddress": "ewestra@rhinofly.nl",
                "avatarUrls": {
                    "16x16": "https://rhinofly.atlassian.net/secure/useravatar?size=small&ownerId=ewestra&avatarId=10200",
                    "48x48": "https://rhinofly.atlassian.net/secure/useravatar?ownerId=ewestra&avatarId=10200"
                },
                "displayName": "Erik Westra [Administrator]",
                "active": true
            },
            "created": "2013-01-08T14:17:07.793+0100",
            "items": [{
                "field": "status",
                "fieldtype": "jira",
                "from": "10004",
                "fromString": "New",
                "to": "10005",
                "toString": "Assigned"
            }, {
                "field": "assignee",
                "fieldtype": "jira",
                "from": "dvink",
                "fromString": "Deef Vink",
                "to": "ewestra",
                "toString": "Erik Westra [Administrator]"
            }]
        }, {
            "id": "13105",
            "author": {
                "self": "https://rhinofly.atlassian.net/rest/api/2/user?username=ewestra",
                "name": "ewestra",
                "emailAddress": "ewestra@rhinofly.nl",
                "avatarUrls": {
                    "16x16": "https://rhinofly.atlassian.net/secure/useravatar?size=small&ownerId=ewestra&avatarId=10200",
                    "48x48": "https://rhinofly.atlassian.net/secure/useravatar?ownerId=ewestra&avatarId=10200"
                },
                "displayName": "Erik Westra [Administrator]",
                "active": true
            },
            "created": "2013-01-08T16:50:50.487+0100",
            "items": [{
                "field": "status",
                "fieldtype": "jira",
                "from": "10005",
                "fromString": "Assigned",
                "to": "10005",
                "toString": "Assigned"
            }]
        }, {
            "id": "13106",
            "author": {
                "self": "https://rhinofly.atlassian.net/rest/api/2/user?username=ewestra",
                "name": "ewestra",
                "emailAddress": "ewestra@rhinofly.nl",
                "avatarUrls": {
                    "16x16": "https://rhinofly.atlassian.net/secure/useravatar?size=small&ownerId=ewestra&avatarId=10200",
                    "48x48": "https://rhinofly.atlassian.net/secure/useravatar?ownerId=ewestra&avatarId=10200"
                },
                "displayName": "Erik Westra [Administrator]",
                "active": true
            },
            "created": "2013-01-08T16:51:20.014+0100",
            "items": [{
                "field": "status",
                "fieldtype": "jira",
                "from": "10005",
                "fromString": "Assigned",
                "to": "3",
                "toString": "In Progress"
            }]
        }, {
            "id": "13109",
            "author": {
                "self": "https://rhinofly.atlassian.net/rest/api/2/user?username=ewestra",
                "name": "ewestra",
                "emailAddress": "ewestra@rhinofly.nl",
                "avatarUrls": {
                    "16x16": "https://rhinofly.atlassian.net/secure/useravatar?size=small&ownerId=ewestra&avatarId=10200",
                    "48x48": "https://rhinofly.atlassian.net/secure/useravatar?ownerId=ewestra&avatarId=10200"
                },
                "displayName": "Erik Westra [Administrator]",
                "active": true
            },
            "created": "2013-01-08T16:52:21.769+0100",
            "items": [{
                "field": "status",
                "fieldtype": "jira",
                "from": "3",
                "fromString": "In Progress",
                "to": "10007",
                "toString": "Waiting for Customer"
            }]
        }]
    }
}"""

    val sladiator = convert(jira, "rhinofly.atlassian.net")

    c(sladiator, Json.parse("""
      {
      	"resolution_date":null,
      	"priority":"Major",
      	"assignee":"ewestra",
      	"source":"Jira webhook -> Sladiator connector by Rhinofly",
      	"issue_updated_at":"2013-01-08 17:24:12 +0100",
      	"url":"https://rhinofly.atlassian.net/browse/CSOI-9",
      	"project":"CSOI",
      	"issue_type":"Bug",
      	"components":[],
      	"key":"CSOI-9",
      	"labels":[],
      	"assignee_avatar_url":"https://rhinofly.atlassian.net/secure/useravatar?ownerId=ewestra&avatarId=10200",
      	"status":"Waiting for Customer",
      	"assignee_email":"ewestra@rhinofly.nl",
      	"due_date":null,
      	"issue_created_at":"2013-01-08 14:13:03 +0100",
      	"transitions": [
    		{
      			"entered_at" : "2013-01-08 14:13:03 +0100",
      			"status" : "New",
    			"exited_at" : "2013-01-08 14:17:07 +0100",
    			"status_to" : "Assigned"
    		},
    		{
    			"entered_at" : "2013-01-08 14:17:07 +0100",
    			"status" : "Assigned",
    			"exited_at" : "2013-01-08 16:50:50 +0100",
    			"status_to" : "Assigned"
    		},
    		{
    			"entered_at" : "2013-01-08 16:50:50 +0100",
    			"status" : "Assigned",
    			"exited_at" : "2013-01-08 16:51:20 +0100",
    			"status_to" : "In Progress"
    		},
    		{
    			"entered_at" : "2013-01-08 16:51:20 +0100",
    			"status" : "In Progress",
    			"exited_at" : "2013-01-08 16:52:21 +0100",
    			"status_to" : "Waiting for Customer"
    		},
    		{
    			"entered_at" : "2013-01-08 16:52:21 +0100",
    			"status" : "Waiting for Customer"
    		}
    	]
      }""")) must beRight
  }

  def testEmptyValue = {
    val testDate = JsString("")

    toSladiatorDate(testDate) must beLike {
      case JsNull => ok
    }
  }

  def testToSladiatorDate = {
    val testDate = JsString("2012-12-14T15:59:09.145+0100")

    val sladiatorDate = toSladiatorDate(testDate)

    sladiatorDate === JsString("2012-12-14 15:59:09 +0100")
  }
}
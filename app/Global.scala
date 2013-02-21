import play.api.GlobalSettings
import play.api.mvc.RequestHeader
import fly.play.jiraExceptionProcessor.JiraExceptionProcessor

object Global extends GlobalSettings {
	override def onError(request:RequestHeader, ex:Throwable) = {
	  JiraExceptionProcessor.reportError(request, ex)
	  super.onError(request, ex)
	}
}
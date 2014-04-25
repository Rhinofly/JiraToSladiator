Jira to Sladiator
=================

Provides a proxy from Jira webhook to Sladiator API

Running this project
====================

In order to run this project you need the following configuration in `JiraToSladiator.conf`:

``` scala
# Jira information, needed to report the errors to Jira
jira.username=username
jira.password="password"
jira.endpoint="https://jira.url/rest/api/2/"

# Information needed by the exception processor
jira.exceptionProcessor.enabled=true
jira.exceptionProcessor.projectKey=PA
jira.exceptionProcessor.componentName=jira_to_sladiator
# Hash is the default
#jira.exceptionProcessor.hashCustomFieldName=Hash
# 1 is the default (Bug)
#jira.exceptionProcessor.issueType=1

# Used when the connection to Jira failed, note that the error is also logged
jira.exceptionProcessor.mail.from.name=Play application
jira.exceptionProcessor.mail.from.address="noreply@company.net"
jira.exceptionProcessor.mail.to.name=Play
jira.exceptionProcessor.mail.to.address="play+error@company.nl"

# Used by the SES plugin
mail.smtp.failTo="failto+test@rhinofly.net"

mail.smtp.host=email-smtp.us-east-1.amazonaws.com
mail.smtp.port=465
mail.smtp.username="username"
mail.smtp.password="password"
```

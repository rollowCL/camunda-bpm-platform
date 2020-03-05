{
  <@lib.endpointInfo
      id = "evaluateCondition"
      tag = "Condition"
      desc = "Triggers evaluation of conditions for conditional start event(s). 
      Internally this maps to the engines condition evaluation builder method ConditionEvaluationBuilder#evaluateStartConditions(). 
      For more information see the [Conditional Start Events](https://docs.camunda.org/manual/latest/reference/bpmn20/events/conditional-events/#conditional-start-event) section of the [BPMN 2.0 Implementation Reference](https://docs.camunda.org/manual/latest/reference/bpmn20/)." />

  <@lib.requestBody
      mediaType = "application/json"
      dto = "EvaluationConditionDto" />

  "responses": {
    <@lib.response
        code = "200"
        dto = "ProcessInstanceDto"
        desc = "Request successful."/>

    <@lib.response
        code = "400"
        dto = "ParseExceptionDto"
        desc = "If both tenantId and withoutTenantId are supplied."/>

    <@lib.response
        code = "403"
        dto = "ParseExceptionDto"
        last = true
        desc = "If the user is not allowed to start the process instance of the process definition, which start condition was evaluted to `true`."/>

  }
}
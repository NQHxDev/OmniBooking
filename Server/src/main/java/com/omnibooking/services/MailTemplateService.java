package com.omnibooking.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class MailTemplateService {

   private final TemplateEngine templateEngine;

   public String buildHtmlContent(String templateName, Map<String, Object> variables) {
      Context context = new Context();
      context.setVariables(variables);
      return templateEngine.process(templateName, context);
   }

}

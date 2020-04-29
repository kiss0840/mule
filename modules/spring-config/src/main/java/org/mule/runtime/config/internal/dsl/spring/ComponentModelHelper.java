/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.component.Component.ANNOTATIONS_PROPERTY_NAME;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.ERROR_HANDLER;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.ON_ERROR;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.ROUTER;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SCOPE;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SOURCE;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.ON_ERROR_CONTINE_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.ON_ERROR_PROPAGATE_IDENTIFIER;
import static org.mule.runtime.config.internal.dsl.model.extension.xml.MacroExpansionModuleModel.ORIGINAL_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.REDELIVERY_POLICY_IDENTIFIER;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.internal.dsl.model.ExtensionModelHelper;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel2;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.xml.namespace.QName;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;

public class ComponentModelHelper {

  /**
   * Resolves the {@link org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType} from a {@link ComponentModel}.
   *
   * @param componentModel a {@link ComponentAst} that represents a component in the configuration.
   * @param extensionModelHelper helper to access components in extension model
   * @return the componentModel type.
   */
  public static TypedComponentIdentifier.ComponentType resolveComponentType(ComponentAst componentModel,
                                                                            ExtensionModelHelper extensionModelHelper) {
    if (componentModel.getIdentifier().equals(ON_ERROR_CONTINE_IDENTIFIER)
        || componentModel.getIdentifier().equals(ON_ERROR_PROPAGATE_IDENTIFIER)) {
      return ON_ERROR;
    }
    return extensionModelHelper
        .findComponentType(componentModel.getMetadata().getParserAttributes().containsKey(ORIGINAL_IDENTIFIER.toString())
            ? (ComponentIdentifier) componentModel.getMetadata().getParserAttributes().get(ORIGINAL_IDENTIFIER.toString())
            : componentModel.getIdentifier());
  }

  public static boolean isAnnotatedObject(SpringComponentModel2 springComponentModel) {
    return isOfType(springComponentModel, Component.class)
        // ValueResolver end up generating pojos from the extension whose class is enhanced to have annotations
        || isOfType(springComponentModel, ValueResolver.class);
  }

  public static boolean isProcessor(ComponentAst componentModel) {
    if (componentModel.getIdentifier().equals(REDELIVERY_POLICY_IDENTIFIER)) {
      return false;
    }
    // return isOfType((ComponentModel) componentModel, Processor.class)
    // || isOfType((ComponentModel) componentModel, InterceptingMessageProcessor.class)
    return false
        || componentModel.getComponentType().equals(OPERATION)
        || componentModel.getComponentType().equals(ROUTER)
        || componentModel.getComponentType().equals(SCOPE);
  }

  public static boolean isMessageSource(ComponentAst componentModel) {
    // return isOfType((ComponentModel) componentModel, MessageSource.class)
    return false
        || componentModel.getComponentType().equals(SOURCE);
  }

  public static boolean isErrorHandler(ComponentAst componentModel) {
    // return isOfType((ComponentModel) componentModel, ErrorHandler.class)
    return false
        || componentModel.getComponentType().equals(ERROR_HANDLER);
  }

  public static boolean isTemplateOnErrorHandler(ComponentAst componentModel) {
    return componentModel.getComponentType().equals(ON_ERROR);
  }

  private static boolean isOfType(SpringComponentModel2 springComponentModel, Class type) {
    Class<?> componentModelType = springComponentModel.getType();
    if (componentModelType == null) {
      return false;
    }
    return CommonBeanDefinitionCreator.areMatchingTypes(type, componentModelType);
  }

  public static void addAnnotation(QName annotationKey, Object annotationValue, SpringComponentModel2 springComponentModel) {
    // TODO MULE-10970 - remove condition once everything is AnnotatedObject.
    if (!ComponentModelHelper.isAnnotatedObject(springComponentModel)
        && !springComponentModel.getComponent().getIdentifier().getName().equals("flow-ref")) {
      return;
    }
    BeanDefinition beanDefinition = springComponentModel.getBeanDefinition();
    if (beanDefinition == null) {
      // This is the case of components that are references
      return;
    }
    updateAnnotationValue(annotationKey, annotationValue, beanDefinition);
  }

  public static void updateAnnotationValue(QName annotationKey, Object annotationValue, BeanDefinition beanDefinition) {
    PropertyValue propertyValue =
        beanDefinition.getPropertyValues().getPropertyValue(ANNOTATIONS_PROPERTY_NAME);
    Map<QName, Object> annotations;
    if (propertyValue == null) {
      annotations = new HashMap<>();
      propertyValue = new PropertyValue(ANNOTATIONS_PROPERTY_NAME, annotations);
      beanDefinition.getPropertyValues().addPropertyValue(propertyValue);
    } else {
      annotations = (Map<QName, Object>) propertyValue.getValue();
    }
    annotations.put(annotationKey, annotationValue);
  }

  public static <T> Optional<T> getAnnotation(QName annotationKey, SpringComponentModel componentModel) {
    if (componentModel.getBeanDefinition() == null) {
      return empty();
    }
    PropertyValue propertyValue =
        componentModel.getBeanDefinition().getPropertyValues().getPropertyValue(ANNOTATIONS_PROPERTY_NAME);
    Map<QName, Object> annotations;
    if (propertyValue == null) {
      return empty();
    } else {
      annotations = (Map<QName, Object>) propertyValue.getValue();
      return ofNullable((T) annotations.get(annotationKey));
    }
  }

  public static boolean isRouter(ComponentAst componentModel) {
    // return ComponentLocationVisitor.BATCH_JOB_COMPONENT_IDENTIFIER.equals(componentModel.getIdentifier())
    // || ComponentLocationVisitor.BATCH_PROCESSS_RECORDS_COMPONENT_IDENTIFIER.equals(componentModel.getIdentifier())
    return false
        || componentModel.getComponentType().equals(ROUTER);
  }
}

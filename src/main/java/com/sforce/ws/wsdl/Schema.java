/*
 * Copyright (c) 2013, salesforce.com, inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 *    Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 *    Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 *    the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *    Neither the name of salesforce.com, inc. nor the names of its contributors may be used to endorse or
 *    promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.sforce.ws.wsdl;

import java.util.*;
import java.util.Collection;

import com.sforce.ws.parser.XmlInputStream;
import com.sforce.ws.util.CollectionUtil;

/**
 * This class represents WSDL->definitions->types->schema
 *
 * @author http://cheenath.com
 * @version 1.0
 * @since 1.0  Nov 9, 2005
 */
public class Schema implements Constants {

    private String targetNamespace;
    private String elementFormDefault;
    private String attributeFormDefault;
    private Map<String, ComplexType> complexTypes = new HashMap<String, ComplexType>();
    private Map<String, SimpleType> simpleTypes = new HashMap<String, SimpleType>();
    private Map<String, Element> elements = new HashMap<String, Element>();
    private Map<String, Attribute> attributes = new HashMap<String, Attribute>();
    private Map<String, AttributeGroup> attributeGroups = new HashMap<String, AttributeGroup>();
    
    private Map<String, Integer> anonymousTypes = new HashMap<String, Integer>();
    
    // parent
    private Types types;
    
    public Schema(Types types) {
        this.types = types;
    }
    
    public String getTargetNamespace() {
        return targetNamespace;
    }

    public boolean isElementFormQualified() {
        return "qualified".equals(elementFormDefault);
    }

    public boolean isAttributeFormQualified() {
        return "qualified".equals(attributeFormDefault);
    }

    public void addComplexType(ComplexType type) {
        complexTypes.put(type.getName(), type);
    }

    public void addSimpleType(SimpleType type) {
        simpleTypes.put(type.getName(), type);
    }

    public Collection<ComplexType> getComplexTypes() {
        return complexTypes.values();
    }

    public Collection<SimpleType> getSimpleTypes() {
        return simpleTypes.values();
    }

    public ComplexType getComplexType(String type) {
        return complexTypes.get(type);
    }

    public SimpleType getSimpleType(String type) {
        return simpleTypes.get(type);
    }

    public Element getGlobalElement(String name) {
        return elements.get(name);
    }

    public Iterator<Element> getGlobalElements() {
        return elements.values().iterator();
    }
    
    public Iterator<Attribute> getGlobalAttributes() {
		return attributes.values().iterator();
	}
    
    public Attribute getGlobalAttribute(String name) {
    	return CollectionUtil.findByName(getGlobalAttributes(), name);
    }

    public Iterator<AttributeGroup> getGlobalAttributeGroups() {
		return attributeGroups.values().iterator();
	}
    
    public AttributeGroup getGlobalAttributeGroup(String name) {
    	return CollectionUtil.findByName(getGlobalAttributeGroups(), name);
    }
    
    /**
     * Generate a unique name for an anonymous type
     * @param name
     * @return the name for the unique element
     */
    public String generateUniqueNameForAnonymousType(String elementName) {
    	String name = elementName + "_element";
    	Integer count = anonymousTypes.get(name);
    	if (count == null) {
    		anonymousTypes.put(name, Integer.valueOf(1));
    	} else {
    		int nextSequence = count.intValue() + 1;
    		anonymousTypes.put(name,  Integer.valueOf(nextSequence));
    		name += "_" + nextSequence;
    	}
    	return name;
    }

	public Types getTypes() {
        return types;
    }

    @Override
    public String toString() {
        return "Schema{" +
                "targetNamespace='" + targetNamespace + '\'' +
                ", elementFormDefault='" + elementFormDefault + '\'' +
                ", attributeFormDefault='" + attributeFormDefault + '\'' +
                ", complexTypes=" + complexTypes +
                '}';
    }

    public void read(WsdlParser parser) throws WsdlParseException {
        targetNamespace = parser.getAttributeValue(null, TARGET_NAME_SPACE);
        elementFormDefault = parser.getAttributeValue(null, ELEMENT_FORM_DEFAULT);
        attributeFormDefault = parser.getAttributeValue(null, ATTRIBUTE_FORM_DEFAULT);

        int eventType = parser.getEventType();

        while (true) {
            if (eventType == XmlInputStream.START_TAG) {
                String n = parser.getName();
                String ns = parser.getNamespace();

                if (COMPLEX_TYPE.equals(n) && SCHEMA_NS.equals(ns)) {
                    ComplexType complexType = new ComplexType(this);
                    complexType.read(parser, null);
                    complexTypes.put(complexType.getName(), complexType);
                } else  if (ELEMENT.equals(n) && SCHEMA_NS.equals(ns)) {
                    Element element = new Element(this);
                    element.read(parser);
                    elements.put(element.getName(), element);
                } else  if (SIMPLE_TYPE.equals(n) && SCHEMA_NS.equals(ns)) {
                    SimpleType simpleType = new SimpleType(this);
                    simpleType.read(parser, null);
                    simpleTypes.put(simpleType.getName(), simpleType);
                } else  if (SCHEMA.equals(n) && SCHEMA_NS.equals(ns)) {
                    //skip header
                } else  if ("import".equals(n) && SCHEMA_NS.equals(ns)) {
                    String location = parser.getAttributeValue(null, "schemaLocation");
                    if (location != null) {
                        throw new WsdlParseException("Found schema import from location " + location +
                                ". External schema import not supported");
                    }
                } else if (ANNOTATION.equals(n) && SCHEMA_NS.equals(ns)) {
                    Annotation annotation = new Annotation();
                    annotation.read(parser);
                } else if (ATTRIBUTE.equals(n) && SCHEMA_NS.equals(ns)) {
                    Attribute attribute = new Attribute(this);
                    attribute.read(parser);
                    attributes.put(attribute.getName(), attribute);
                } else if (ATTRIBUTE_GROUP.equals(n) && SCHEMA_NS.equals(ns)) {
                    AttributeGroup attributeGroup = new AttributeGroup(this);
                    attributeGroup.read(parser);
                    attributeGroups.put(attributeGroup.getName(), attributeGroup);
                } else {
                    throw new WsdlParseException("Unsupported Schema element found "
                            + ns + ":" + n + ". At: " + parser.getPositionDescription());
                }
            } else if (eventType == XmlInputStream.END_TAG) {
                String name = parser.getName();
                String namespace = parser.getNamespace();

                if (SCHEMA.equals(name) && SCHEMA_NS.equals(namespace)) {
                    break;
                }
            } else if (eventType == XmlInputStream.END_DOCUMENT) {
                throw new WsdlParseException("Failed to find end tag for 'schema'");
            }

            eventType = parser.next();
        }

        if (targetNamespace == null) {
            throw new WsdlParseException("schema:targetNamespace can not be null");
        }
    }
}

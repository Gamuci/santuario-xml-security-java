/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.xml.security.utils;

import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import org.apache.xml.security.exceptions.Base64DecodingException;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * This is the base class to all Objects which have a direct 1:1 mapping to an
 * Element in a particular namespace.
 */
public abstract class ElementProxy {

    protected static final org.slf4j.Logger log = 
        org.slf4j.LoggerFactory.getLogger(ElementProxy.class);

    /**
     * What XML element does this ElementProxy instance wrap? 
     */
    private Element wrappedElement = null;

    /** Field baseURI */
    protected String baseURI = null;

    /** Field doc */
    private Document wrappedDoc = null;
    
    /** Field prefixMappings */
    private static Map<String, String> prefixMappings = new ConcurrentHashMap<String, String>();

    /**
     * Constructor ElementProxy
     *
     */
    public ElementProxy() {	   
    }

    /**
     * Constructor ElementProxy
     *
     * @param doc
     */
    public ElementProxy(Document doc) {
        if (doc == null) {
            throw new RuntimeException("Document is null");
        }

        this.wrappedDoc = doc;
        this.wrappedElement = createElementForFamilyLocal(this.getBaseNamespace(), this.getBaseLocalName());      
    }
    
    /**
     * Constructor ElementProxy
     *
     * @param element
     * @param BaseURI
     * @throws XMLSecurityException
     */
    public ElementProxy(Element element, String BaseURI) throws XMLSecurityException {
        if (element == null) {
            throw new XMLSecurityException("ElementProxy.nullElement");
        }

        if (log.isDebugEnabled()) {
            log.debug("setElement(\"" + element.getTagName() + "\", \"" + BaseURI + "\")");
        }

        setElement(element);
        this.baseURI = BaseURI;

        this.guaranteeThatElementInCorrectSpace();
    }
    
    /**
     * Returns the namespace of the Elements of the sub-class.
     *
     * @return the namespace of the Elements of the sub-class.
     */
    public abstract String getBaseNamespace();

    /**
     * Returns the localname of the Elements of the sub-class.
     *
     * @return the localname of the Elements of the sub-class.
     */
    public abstract String getBaseLocalName();
    
    
    protected Element createElementForFamilyLocal(
        String namespace, String localName
    ) {
        Document doc = getDocument();
        Element result = null;
        if (namespace == null) {
            result = doc.createElementNS(null, localName);
        } else {
            String baseName = this.getBaseNamespace();
            String prefix = ElementProxy.getDefaultPrefix(baseName);
            if (prefix == null || prefix.length() == 0) {
                result = doc.createElementNS(namespace, localName);
                result.setAttributeNS(Constants.NamespaceSpecNS, "xmlns", namespace);
            } else {
                result = doc.createElementNS(namespace, prefix + ":" + localName);
                result.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:" + prefix, namespace);
            }
        }	      
        return result;
    }


    /**
     * This method creates an Element in a given namespace with a given localname.
     * It uses the {@link ElementProxy#getDefaultPrefix} method to decide whether
     * a particular prefix is bound to that namespace.
     * <BR />
     * This method was refactored out of the constructor.
     *
     * @param doc
     * @param namespace
     * @param localName
     * @return The element created.
     */
    public static Element createElementForFamily(Document doc, String namespace, String localName) {
        Element result = null;
        String prefix = ElementProxy.getDefaultPrefix(namespace);

        if (namespace == null) {
            result = doc.createElementNS(null, localName);
        } else {
            if (prefix == null || prefix.length() == 0) {
                result = doc.createElementNS(namespace, localName);
                result.setAttributeNS(Constants.NamespaceSpecNS, "xmlns", namespace);
            } else {
                result = doc.createElementNS(namespace, prefix + ":" + localName);
                result.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:" + prefix, namespace);
            }
        }

        return result;
    }

    /**
     * Method setElement
     *
     * @param element
     * @param BaseURI
     * @throws XMLSecurityException
     */
    public void setElement(Element element, String BaseURI) throws XMLSecurityException {
        if (element == null) {
            throw new XMLSecurityException("ElementProxy.nullElement");
        }

        if (log.isDebugEnabled()) {
            log.debug("setElement(" + element.getTagName() + ", \"" + BaseURI + "\"");
        }

        setElement(element);
        this.baseURI = BaseURI;
    }

    /**
     * Returns the Element which was constructed by the Object.
     *
     * @return the Element which was constructed by the Object.
     */
    public final Element getElement() {
        return this.wrappedElement;
    }

    /**
     * Returns the Element plus a leading and a trailing CarriageReturn Text node.
     *
     * @return the Element which was constructed by the Object.
     */
    public final NodeList getElementPlusReturns() {

        HelperNodeList nl = new HelperNodeList();

        nl.appendChild(createText("\n"));
        nl.appendChild(getElement());
        nl.appendChild(createText("\n"));

        return nl;
    }

    protected Text createText(String text) {
        return this.wrappedDoc.createTextNode(text);
    }

    /**
     * Method getDocument
     *
     * @return the Document where this element is contained.
     */
    public Document getDocument() {
        if (wrappedDoc == null) {
            wrappedDoc = XMLUtils.getOwnerDocument(wrappedElement);
        }
        return wrappedDoc;
    }

    /**
     * Method getBaseURI
     *
     * @return the base uri of the namespace of this element
     */
    public String getBaseURI() {
        return this.baseURI;
    }

    /**
     * Method guaranteeThatElementInCorrectSpace
     *
     * @throws XMLSecurityException
     */
    void guaranteeThatElementInCorrectSpace() throws XMLSecurityException {

        String expectedLocalName = this.getBaseLocalName();
        String expectedNamespaceUri = this.getBaseNamespace();

        String actualLocalName = getElement().getLocalName();
        String actualNamespaceUri = getElement().getNamespaceURI();

        if(!expectedNamespaceUri.equals(actualNamespaceUri) 
            && !expectedLocalName.equals(actualLocalName)) {      
            Object exArgs[] = { actualNamespaceUri + ":" + actualLocalName, 
                                expectedNamespaceUri + ":" + expectedLocalName};
            throw new XMLSecurityException("xml.WrongElement", exArgs);
        }
    }

    /**
     * Method addBigIntegerElement
     *
     * @param bi
     * @param localname
     */
    public void addBigIntegerElement(BigInteger bi, String localname) {
        if (bi != null) {
            Element e = XMLUtils.createElementInSignatureSpace(getDocument(), localname);

            Base64.fillElementWithBigInteger(e, bi);
            appendSelf(e);
            addReturnToSelf();
        }
    }

    protected void addReturnToSelf() {
        XMLUtils.addReturnToElement(getElement());
    }

    /**
     * Method addBase64Element
     *
     * @param bytes
     * @param localname
     */
    public void addBase64Element(byte[] bytes, String localname) {
        if (bytes != null) {
            Element e = Base64.encodeToElement(getDocument(), localname, bytes);

            appendSelf(e);
            if (!XMLUtils.ignoreLineBreaks()) {
                appendSelf(createText("\n"));
            }
        }
    }

    /**
     * Method addTextElement
     *
     * @param text
     * @param localname
     */
    public void addTextElement(String text, String localname) {
        Element e = XMLUtils.createElementInSignatureSpace(getDocument(), localname);
        Text t = createText(text);

        appendOther(e, t);
        appendSelf(e);
        addReturnToSelf();
    }

    /**
     * Method addBase64Text
     *
     * @param bytes
     */
    public void addBase64Text(byte[] bytes) {
        if (bytes != null) {
            Text t = XMLUtils.ignoreLineBreaks() 
                ? createText(Base64.encode(bytes))
                : createText("\n" + Base64.encode(bytes) + "\n");
            appendSelf(t);
        }
    }

    protected void appendSelf(ElementProxy toAppend) {
        getElement().appendChild(toAppend.getElement());
    }
    
    protected void appendSelf(Node toAppend) {
        getElement().appendChild(toAppend);
    }
    
    protected void appendOther(Element parent, Node toAppend) {
        parent.appendChild(toAppend);
    }
    
    /**
     * Method addText
     *
     * @param text
     */
    public void addText(String text) {
        if (text != null) {
            Text t = createText(text);

            appendSelf(t);
        }
    }

    /**
     * Method getVal
     *
     * @param localname
     * @param namespace
     * @return The biginteger contained in the given element
     * @throws Base64DecodingException
     */
    public BigInteger getBigIntegerFromChildElement(
        String localname, String namespace
    ) throws Base64DecodingException {
        return Base64.decodeBigIntegerFromString(
            XMLUtils.selectNodeText(
                getFirstChild(), namespace, localname, 0
            ).getNodeValue()
        );
    }

    /**
     * Method getTextFromChildElement
     *
     * @param localname
     * @param namespace
     * @return the Text of the textNode
     */
    public String getTextFromChildElement(String localname, String namespace) {
        return XMLUtils.selectNode(
                getFirstChild(),
                namespace,
                localname,
                0).getTextContent();
    }

    /**
     * Method getBytesFromTextChild
     *
     * @return The base64 bytes from the text children of this element
     * @throws XMLSecurityException
     */
    public byte[] getBytesFromTextChild() throws XMLSecurityException {
        return Base64.decode(getTextFromTextChild());
    }

    /**
     * Method getTextFromTextChild
     *
     * @return the Text obtained by concatenating all the text nodes of this 
     *    element
     */
    public String getTextFromTextChild() {
        return XMLUtils.getFullTextChildrenFromElement(getElement());
    }

    /**
     * Method length
     *
     * @param namespace
     * @param localname
     * @return the number of elements {namespace}:localname under this element
     */
    public int length(String namespace, String localname) {
        int number = 0;
        Node sibling = getFirstChild();
        while (sibling != null) {        
            if (localname.equals(sibling.getLocalName())
                && namespace.equals(sibling.getNamespaceURI())) {
                number++;
            }
            sibling = sibling.getNextSibling();
        }
        return number;
    }

    /**
     * Adds an xmlns: definition to the Element. This can be called as follows:
     *
     * <PRE>
     * // set namespace with ds prefix
     * xpathContainer.setXPathNamespaceContext("ds", "http://www.w3.org/2000/09/xmldsig#");
     * xpathContainer.setXPathNamespaceContext("xmlns:ds", "http://www.w3.org/2000/09/xmldsig#");
     * </PRE>
     *
     * @param prefix
     * @param uri
     * @throws XMLSecurityException
     */
    public void setXPathNamespaceContext(String prefix, String uri)
        throws XMLSecurityException {
        String ns;

        if (prefix == null || prefix.length() == 0) {
            throw new XMLSecurityException("defaultNamespaceCannotBeSetHere");
        } else if (prefix.equals("xmlns")) {
            throw new XMLSecurityException("defaultNamespaceCannotBeSetHere");
        } else if (prefix.startsWith("xmlns:")) {
            ns = prefix;//"xmlns:" + prefix.substring("xmlns:".length());
        } else {
            ns = "xmlns:" + prefix;
        }

        Attr a = getElement().getAttributeNodeNS(Constants.NamespaceSpecNS, ns);

        if (a != null) { 
            if (!a.getNodeValue().equals(uri)) {
                Object exArgs[] = { ns, getElement().getAttributeNS(null, ns) };

                throw new XMLSecurityException("namespacePrefixAlreadyUsedByOtherURI", exArgs);
            }
            return;
        }

        getElement().setAttributeNS(Constants.NamespaceSpecNS, ns, uri);
    }

    /**
     * Method setDefaultPrefix
     *
     * @param namespace
     * @param prefix
     * @throws XMLSecurityException
     */
    public static void setDefaultPrefix(String namespace, String prefix)
        throws XMLSecurityException {
        if (prefixMappings.containsValue(prefix)) {
            String storedPrefix = prefixMappings.get(namespace);
            if (!storedPrefix.equals(prefix)) {
                Object exArgs[] = { prefix, namespace, storedPrefix };

                throw new XMLSecurityException("prefix.AlreadyAssigned", exArgs);
            }
        }
        
        if (Constants.SignatureSpecNS.equals(namespace)) {
            XMLUtils.setDsPrefix(prefix);
        }
        if (EncryptionConstants.EncryptionSpecNS.equals(namespace)) {
            XMLUtils.setXencPrefix(prefix);
        }
        prefixMappings.put(namespace, prefix);
    }
    
    /**
     * This method registers the default prefixes.
     */
    public static void registerDefaultPrefixes() throws XMLSecurityException {
        setDefaultPrefix("http://www.w3.org/2000/09/xmldsig#", "ds");
        setDefaultPrefix("http://www.w3.org/2001/04/xmlenc#", "xenc");
        setDefaultPrefix("http://www.w3.org/2009/xmlenc11#", "xenc11");
        setDefaultPrefix("http://www.xmlsecurity.org/experimental#", "experimental");
        setDefaultPrefix("http://www.w3.org/2002/04/xmldsig-filter2", "dsig-xpath-old");
        setDefaultPrefix("http://www.w3.org/2002/06/xmldsig-filter2", "dsig-xpath");
        setDefaultPrefix("http://www.w3.org/2001/10/xml-exc-c14n#", "ec");
        setDefaultPrefix(
            "http://www.nue.et-inf.uni-siegen.de/~geuer-pollmann/#xpathFilter", "xx"
        );
    }

    /**
     * Method getDefaultPrefix
     *
     * @param namespace
     * @return the default prefix bind to this element.
     */
    public static String getDefaultPrefix(String namespace) {
        return prefixMappings.get(namespace);
    }

    /**
     * New value for the wrapped XML element that this object is a proxy for.
     * 
     * @param elem  New element
     * 
     * @see #getWrappedElement()
     */
    protected void setElement(Element elem) {
        wrappedElement = elem;
    }
    
    /**
     * Set a new value for the wrapped document that this object is a proxy for.
     * 
     * @param doc New document object being wrapped.
     * 
     * @see #getWrappedDocument()
     */
    protected void setDocument(Document doc) {
        wrappedDoc = doc;
    }
    
    protected String getLocalAttribute(String attrName) {
        return getElement().getAttributeNS(null, attrName);
    }
    
    protected void setLocalAttribute(String attrName, String value) {
        getElement().setAttributeNS(null, attrName, value);
    }
    
    protected void setLocalIdAttribute(String attrName, String value) {
        
        if (value != null) {
            Attr attr = getDocument().createAttributeNS(null, attrName);
            attr.setValue(value);
            getElement().setAttributeNodeNS(attr);
            getElement().setIdAttributeNode(attr, true);
        }
        else {
            getElement().removeAttributeNS(null, attrName);
        }
    }
    
    protected Node getFirstChild() {
        return getElement().getFirstChild();
    }
    
}

package org.imixs.muluk.xml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * 
 * @author rsoika
 *
 */
@XmlRootElement(name = "object")
public class XMLObject implements java.io.Serializable {

	@XmlTransient
	private static final long serialVersionUID = 1L;
	private String type;

	private String target;
	private String result;

	public XMLObject() {
		super();
	}

	@XmlAttribute
	public void setType(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

}

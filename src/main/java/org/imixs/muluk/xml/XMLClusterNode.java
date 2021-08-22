package org.imixs.muluk.xml;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * 
 * @author rsoika
 *
 */
@XmlRootElement(name = "node")
public class XMLClusterNode implements java.io.Serializable {

	@XmlTransient
	private static final long serialVersionUID = 1L;

	private String label;

	public XMLClusterNode() {
		super();
	}

	/**
	 * Returns the name of the entity.
	 * 
	 * @return - name of this entity
	 */
	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

}

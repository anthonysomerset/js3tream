/**
 * Permission.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.amazonaws.s3.doc._2006_03_01;

public class Permission implements java.io.Serializable {
    private java.lang.String _value_;
    private static java.util.HashMap _table_ = new java.util.HashMap();

    // Constructor
    protected Permission(java.lang.String value) {
        _value_ = value;
        _table_.put(_value_,this);
    }

    public static final java.lang.String _READ = "READ";
    public static final java.lang.String _WRITE = "WRITE";
    public static final java.lang.String _READ_ACP = "READ_ACP";
    public static final java.lang.String _WRITE_ACP = "WRITE_ACP";
    public static final java.lang.String _FULL_CONTROL = "FULL_CONTROL";
    public static final Permission READ = new Permission(_READ);
    public static final Permission WRITE = new Permission(_WRITE);
    public static final Permission READ_ACP = new Permission(_READ_ACP);
    public static final Permission WRITE_ACP = new Permission(_WRITE_ACP);
    public static final Permission FULL_CONTROL = new Permission(_FULL_CONTROL);
    public java.lang.String getValue() { return _value_;}
    public static Permission fromValue(java.lang.String value)
          throws java.lang.IllegalArgumentException {
        Permission enumeration = (Permission)
            _table_.get(value);
        if (enumeration==null) throw new java.lang.IllegalArgumentException();
        return enumeration;
    }
    public static Permission fromString(java.lang.String value)
          throws java.lang.IllegalArgumentException {
        return fromValue(value);
    }
    public boolean equals(java.lang.Object obj) {return (obj == this);}
    public int hashCode() { return toString().hashCode();}
    public java.lang.String toString() { return _value_;}
    public java.lang.Object readResolve() throws java.io.ObjectStreamException { return fromValue(_value_);}
    public static org.apache.axis.encoding.Serializer getSerializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new org.apache.axis.encoding.ser.EnumSerializer(
            _javaType, _xmlType);
    }
    public static org.apache.axis.encoding.Deserializer getDeserializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new org.apache.axis.encoding.ser.EnumDeserializer(
            _javaType, _xmlType);
    }
    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(Permission.class);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://s3.amazonaws.com/doc/2006-03-01/", "Permission"));
    }
    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

}

package com.flat502.rox.marshal;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Defines an interface for accessing members of a Java 
 * {@link java.lang.Class} consistently.
 * <p>
 * An instance of this class provides uniform access to members
 * of a Java Class. Whether or not the member is a public
 * field or only through methods is opaque to the caller.
 * <p>
 * This class guarantees a singleton instance per Class.
 * As such instances of this class are only available via
 * the static {@link #getInstance(Class) factory method}.
 * <p>
 * The set of members on a given class is defined as
 * all non-transient public fields and all properties
 * (in the {@link java.beans.Introspector}
 * sense) accessible via either a public getter or setter 
 * method. If there is an overlap (i.e. a setter or getter
 * exists for a public field) methods are given precedence.
 */
public class ClassDescriptor {
	private static Map cache = new HashMap();

	// TODO: How do we cope with method overloading?
	private Map fields = new HashMap();
	private Map getters = new HashMap();
	private Map setters = new HashMap();

	// These have slightly slower access times than a HashSet
	// but for the numbers of members we are dealing with this
	// is a non-issue. Order makes testing simpler.
	private Set getterNames = new TreeSet();
	private Set setterNames = new TreeSet();

	private Class clazz;

	private ClassDescriptor(Class clazz) throws IntrospectionException {
		this.clazz = clazz;
		this.inspect(clazz);
	}

	/**
	 * Sets the value of the named property.
	 * <p>
	 * No type coercion beyond that provided by 
	 * {@link Field#set(java.lang.Object, java.lang.Object)} or
	 * {@link Method#invoke(java.lang.Object, java.lang.Object[])}
	 * is performed.
	 * @param target
	 * 	The object on which the named property should be set.
	 * @param name
	 * 	The name of the property to set.
	 * @param value
	 * 	The value to set the named property to.
	 * @throws IllegalArgumentException
	 * 	if <code>target</code> or <code>name</code> are <code>null</code>, or
	 * 	if the {@link Class} represented by this instance differs to
	 * 	<code>target</code>'s.
	 * @throws IllegalAccessException
	 * 	if the field or method associated with the named
	 * 	property is inaccessible.
	 * @throws InvocationTargetException
	 * 	if an exception is raised while invoking a setter.
	 */
	public void setValue(Object target, String name, Object value) throws IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {
		this.validateAccessorArgs(target, name);

		//  Methods take precendence when there's an overlap.
		Method setter = (Method) this.setters.get(name);
		if (setter != null) {
			setter.invoke(target, new Object[] { value });
			return;
		}

		Field field = (Field) this.fields.get(name);
		if (field != null) {
			field.set(target, value);
			return;
		}

		throw new IllegalArgumentException("No setter for " + name);
	}

	/**
	 * Gets the value of the named property.
	 * @param target
	 * 	The object on which the named property should be set.
	 * @param name
	 * 	The name of the property to set.
	 * @return
	 * 	the current value of the named property on the
	 * 	target object.
	 * @throws IllegalArgumentException
	 * 	if <code>target</code> or <code>name</code> are <code>null</code>, or
	 * 	if the {@link Class} represented by this instance differs to
	 * 	<code>target</code>'s.
	 * @throws IllegalAccessException
	 * 	if the field or method associated with the named
	 * 	property is inaccessible.
	 * @throws InvocationTargetException
	 * 	if an exception is raised while invoking a setter.
	 */
	public Object getValue(Object target, String name) throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {
		this.validateAccessorArgs(target, name);

		// Methods take precendence when there's an overlap.
		Method getter = (Method) this.getters.get(name);
		if (getter != null) {
			return getter.invoke(target, (Object[])null);
		}

		Field field = (Field) this.fields.get(name);
		if (field != null) {
			return field.get(target);
		}

		throw new IllegalArgumentException("No getter for " + name);
	}

	/**
	 * Provides an {@link java.util.Iterator} over the getters on the represented
	 * {@link Class}.
	 * <p>
	 * The iterator returns {@link String} instances on each
	 * iteration. These instances name the readable properties on
	 * the represented Class.
	 * @return 
	 * 	an {@link java.util.Iterator} over the getters available on the
	 * 	represented {@link Class}.
	 */
	public Iterator getters() {
		return this.getterNames.iterator();
	}

	/**
	 * Provides an {@link java.util.Iterator} over the setters on the represented
	 * {@link Class}.
	 * <p>
	 * The iterator returns {@link String} instances on each
	 * iteration. These instances name the writeable properties on
	 * the represented Class.
	 * @return 
	 * 	an {@link java.util.Iterator} over the setters available on the
	 * 	represented {@link Class}.
	 */
	public Iterator setters() {
		return this.setterNames.iterator();
	}

	public Class getGetterType(String name) {
		if (name == null) {
			throw new IllegalArgumentException("name is null");
		}

		//  Methods take precendence when there's an overlap.
		Method getter = (Method) this.getters.get(name);
		if (getter != null) {
			return getter.getReturnType();
		}

		Field field = (Field) this.fields.get(name);
		if (field != null) {
			return field.getType();
		}

		throw new IllegalArgumentException("No getter for " + name);
	}

	public Class getSetterType(String name) {
		if (name == null) {
			throw new IllegalArgumentException("name is null");
		}

		//  Methods take precendence when there's an overlap.
		Method setter = (Method) this.setters.get(name);
		if (setter != null) {
			return setter.getParameterTypes()[0];
		}

		Field field = (Field) this.fields.get(name);
		if (field != null) {
			return field.getType();
		}

		throw new IllegalArgumentException("No setter for " + name + " on " + clazz.getName());
	}

	private void validateAccessorArgs(Object target, String name) {
		if (target == null) {
			throw new IllegalArgumentException("target is null");
		}
		if (target.getClass() != this.clazz) {
			throw new IllegalArgumentException("Wrong class: " + target.getClass().getName() + " != "
					+ this.clazz.getName());
		}
		if (name == null) {
			throw new IllegalArgumentException("name is null");
		}
	}

	private void inspect(Class clazz) throws IntrospectionException {
		// Look for public fields.
		Field[] pubFields = clazz.getFields();

		// Filter out
		// 1. transient fields
		// 2. static fields
		// 3. final fields
		for (int i = 0; i < pubFields.length; i++) {
			int modifiers = pubFields[i].getModifiers();
			if ((modifiers & Modifier.TRANSIENT) != 0) {
				continue;
			}
			if ((modifiers & Modifier.STATIC) != 0) {
				continue;
			}
			if ((modifiers & Modifier.FINAL) != 0) {
				continue;
			}
			this.fields.put(pubFields[i].getName(), pubFields[i]);
			this.getterNames.add(pubFields[i].getName());
			this.setterNames.add(pubFields[i].getName());
		}

		// Introspect for getter and setter methods.
		BeanInfo info = Introspector.getBeanInfo(clazz, Object.class);

		PropertyDescriptor[] props = info.getPropertyDescriptors();
		for (int i = 0; i < props.length; i++) {
			if (props[i].getReadMethod() != null) {
				this.getters.put(props[i].getName(), props[i].getReadMethod());
				this.getterNames.add(props[i].getName());
			}
			if (props[i].getWriteMethod() != null) {
				this.setters.put(props[i].getName(), props[i].getWriteMethod());
				this.setterNames.add(props[i].getName());
			}
		}
	}

	/**
	 * Static factory method for creating instances of this class.
	 * <p>
	 * Multiple invocations of this method with the same {@link Class}
	 * instance are guaranteed to return the same instance.
	 * @param clazz
	 * 	The class for which a {@link ClassDescriptor} is required.
	 * @return
	 * 	The {@link ClassDescriptor} instance associated with the
	 * 	given class.
	 * @throws IntrospectionException
	 * 	if an error ocurrs while introspecting the given class.
	 * @throws IllegalArgumentException
	 * 	if the parameter is <code>null</code>.
	 */
	public static ClassDescriptor getInstance(Class clazz) throws IntrospectionException {
		if (clazz == null) {
			throw new IllegalArgumentException("null clazz");
		}
		synchronized (cache) {
			ClassDescriptor descriptor = (ClassDescriptor) cache.get(clazz);
			if (descriptor == null) {
				descriptor = new ClassDescriptor(clazz);
				cache.put(clazz, descriptor);
			}
			return descriptor;
		}
	}

    public String toString() { return "[CD for " + clazz.getName() + "]"; }
}

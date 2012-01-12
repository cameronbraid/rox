package com.flat502.rox.marshal;

public class HyphenatedFieldNameCodec implements FieldNameCodec {
	/**
	 * Determine the name of an XML-RPC struct member from an Object field name.
	 * <p>
	 * The default implementation splits the name on uppercase characters,
	 * converts the elements in the resulting list to lowercase and joins the
	 * resulting list using the '-' character.
	 * 
	 * @param name
	 *            The name of the Object field.
	 * @return The XML-RPC struct member name the associated value should be
	 *         assigned to.
	 */
	public String encodeFieldName(String name) {
		StringBuffer sb = new StringBuffer();
		int startIdx = 0;
		for (int i = 0; i < name.length(); i++) {
			if (Character.isUpperCase(name.charAt(i))) {
				if (i > 0) {
					sb.append(name.substring(startIdx, i).toLowerCase());
					sb.append('-');
					startIdx = i;
				}
			}
		}
		sb.append(name.substring(startIdx).toLowerCase());
		return sb.toString();
	}

	/**
	 * Determine an Object field name from the name of an XML-RPC struct member.
	 * <p>
	 * The default implementation splits the name on the '-' character and
	 * concatenates the elements in the resulting list after converting the
	 * first character of all but the first elements to it's uppercase
	 * equivalent. All other characters are converted to lowercase.
	 * 
	 * @param name
	 *            The XML-RPC struct member name.
	 * @return The name of the field the associated value should be assigned to.
	 */
	public String decodeFieldName(String name) {
		StringBuffer sb = new StringBuffer();
		int startIdx = 0;
		boolean upcase = false;
		for (int i = 0; i < name.length(); i++) {
			char ch = name.charAt(i);
			if (ch == '-') {
				upcase = true;
				continue;
			}
			if (upcase) {
				sb.append(Character.toUpperCase(ch));
			} else {
				sb.append(Character.toLowerCase(ch));
			}
			upcase = false;
		}
		return sb.toString();
	}
}

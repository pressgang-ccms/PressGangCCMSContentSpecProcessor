package com.redhat.contentspec.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionUtilities extends
		com.redhat.ecs.commonutils.ExceptionUtilities {

	public static String getStackTrace(Throwable t) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw, true);
		t.printStackTrace(pw);
		pw.flush();
		sw.flush();
		return sw.toString();
	}
}

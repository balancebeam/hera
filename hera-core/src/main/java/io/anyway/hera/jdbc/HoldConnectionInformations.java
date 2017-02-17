/*
 * Copyright 2008-2016 by Emeric Vernat
 *
 *     This file is part of Java Melody.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.anyway.hera.jdbc;

import java.io.Serializable;
import java.sql.Connection;
import java.util.*;

public class HoldConnectionInformations implements Serializable {
	private static final long serialVersionUID = -6063966419161604125L;
	private static final String OWN_PACKAGE = HoldConnectionInformations.class.getName().substring(0,
			HoldConnectionInformations.class.getName().lastIndexOf('.'));

	static List<String> HOLD_INTEREST_TRACE_PACKAGES = Collections.EMPTY_LIST;
	private final long openingTime;
	private final StackTraceElement[] openingStackTrace;
	private final long threadId;

	HoldConnectionInformations() {
		this.openingTime = System.currentTimeMillis();
		final Thread currentThread = Thread.currentThread();
		this.openingStackTrace = currentThread.getStackTrace();
		this.threadId = currentThread.getId();
	}

	static int getUniqueIdOfConnection(Connection connection) {
		return System.identityHashCode(connection);
	}

	long getOpeningTime() {
		return openingTime;
	}

	List<StackTraceElement> getOpeningStackTrace() {
		if (openingStackTrace == null) {
			return Collections.emptyList();
		}
		final List<StackTraceElement> stackTrace = new ArrayList<StackTraceElement>(
				Arrays.asList(openingStackTrace));
		stackTrace.remove(0);
		while (stackTrace.get(0).getClassName().startsWith(OWN_PACKAGE)) {
			stackTrace.remove(0);
		}
		if(!HOLD_INTEREST_TRACE_PACKAGES.isEmpty()){
			for(int i=stackTrace.size()-1;i>=0;i--){
				String clsName= stackTrace.get(i).getClassName();
				for(String each: HOLD_INTEREST_TRACE_PACKAGES){
					if(!clsName.startsWith(each)){
						stackTrace.remove(i);
						break;
					}
				}
			}
		}
		return stackTrace;
	}

	String getMatchingHoldService(){
		if(openingStackTrace== null){
			return null;
		}
		for(int i=6,l=openingStackTrace.length;i<l;i++){
			String clsName= openingStackTrace[i].getClassName();
			for(String each: HOLD_INTEREST_TRACE_PACKAGES){
				if(clsName.startsWith(each) && !clsName.contains("$")){
					return clsName.substring(clsName.lastIndexOf(".")+1)+ "."+ openingStackTrace[i].getMethodName();
				}
			}
		}
		return null;
	}


	long getThreadId() {
		return threadId;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[openingDate=" + new Date(getOpeningTime()) + ", threadId="
				+ getThreadId() + ", stackTrace="+getOpeningStackTrace()+']';
	}
}

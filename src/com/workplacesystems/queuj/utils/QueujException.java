/*
 * Copyright 2010 Workplace Systems PLC (http://www.workplacesystems.com/).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.workplacesystems.queuj.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author dave
 */
public class QueujException extends RuntimeException
{
    private final static Log log = LogFactory.getLog(QueujException.class);

    private final static String new_line = System.getProperty("line.separator");

    /**
     *
     */
    public QueujException()
    {
        this(null, null);
    }

    /**
     *
     * @param e
     */
    public QueujException(Exception e)
    {
        this(e.getMessage(), e);
    }

    /**
     *
     * @param response
     */
    public QueujException(String response)
    {
        this(response, null);
    }

    /**
     *
     * @param response
     * @param e
     */
    public QueujException(String response, Exception e)
    {
        super(response, e);

        String message = getMessage();

        Throwable traced_exception = e;
        while (traced_exception instanceof InvocationTargetException)
        {
            InvocationTargetException ite = (InvocationTargetException) traced_exception;
            traced_exception = ite.getTargetException();
        }
        if (traced_exception instanceof QueujException)
            return;

        StringWriter sw = new StringWriter();
        PrintWriter pr = new PrintWriter(sw);
        printStackTrace(pr);
        message += new_line + new_line + sw.toString();

        log.fatal(message);
    }
}

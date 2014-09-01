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

package com.workplacesystems.queuj;

import java.util.HashMap;

/**
 * ProcessOutput provides static variables of the supported output types
 * that a Process can produce and methods to check a type is supported and
 * to get the mime type for the output type.
 *
 * @author dave
 */
public final class ProcessOutput
{
    /** PDF output type. */
    public static final String PDF_TYPE = "PDF";

    /** CSV output type. */
    public static final String CSV_TYPE = "CSV";

    /** PDF mime type. */
    private static final String PDF_MIME_TYPE = "application/pdf";

    /** Text mime type. */
    private static final String TEXT_MIME_TYPE = "text/plain";

    /** Map of output types and mime types. */
    private final static HashMap mime_types = new HashMap();

    static
    {
        // Set the mime types for the supported output types
        mime_types.put(PDF_TYPE, PDF_MIME_TYPE);
        mime_types.put(CSV_TYPE, TEXT_MIME_TYPE);
    }

    private ProcessOutput() {}

    /**
     * Get the mime type for the supplied output type.
     */
    public static String getMimeType(String report_type)
    {
        return (String)mime_types.get(report_type);
    }

    /**
     * Is the supplied output type supported.
     */
    public final static boolean isSupportedType(String type)
    {
        return mime_types.containsKey(type);
    }
}

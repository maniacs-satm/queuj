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

package com.workplacesystems.queuj.process;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

import java.util.Map;

/**
 *
 * @author dave
 */
public interface ProcessOutputable
{
    void setDetails(String user_id, String source_name, String output_name, String report_title, String report_type, Map<String,Object> options);

    String getOutputName();

    void updatePDF(String mime_type, byte[] report_bytes);

    String getStream(Locale locale);

    String getReportShortTimestamp(Locale locale);

    String getReportTitle(Locale locale);

    String getReportType(Locale locale);

    boolean isViewable();

    void writePDF(OutputStream out) throws IOException;

    String getReportOutputName(Locale locale);

    int getReportLength();

    String getMimeType();
}

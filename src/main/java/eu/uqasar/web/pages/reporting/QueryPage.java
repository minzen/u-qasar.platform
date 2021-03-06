package eu.uqasar.web.pages.reporting;

/*
 * #%L
 * U-QASAR
 * %%
 * Copyright (C) 2012 - 2015 U-QASAR Consortium
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import eu.uqasar.util.reporting.Util;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 *
 */
public class QueryPage extends WebPage {

    public QueryPage(final PageParameters parameters) {
        super(parameters);
        try {
            HttpServletRequest request = (HttpServletRequest) ((WebRequest) getRequest()).getContainerRequest();
            HttpServletResponse response = (HttpServletResponse) ((WebResponse) getResponse()).getContainerResponse();
            //generate cube output

            JSONObject objToReturn = new JSONObject();
            response.setContentType("text/html;charset=UTF-8");

            String cubeurl = request.getParameter("data[cube]");
            String urlToLoad = Util.constructCubeRetrieverURL(request.getParameter("data[rules]"), cubeurl);
            String urlToLoadAsLink = "<a href='" + urlToLoad + "'>" + urlToLoad + "</a>";

            JSONObject cuberesponse = Util.readJsonFromUrl(urlToLoad);
            JSONObject donutchartJSON = new JSONObject();

            if (cuberesponse.has("error")) {
                System.out.println("Exception during retrieval !");
                objToReturn.put("error", cuberesponse.get("error"));
            } else {

                JSONArray cuberesponse_arr = cuberesponse.getJSONArray("cells");

                if (cuberesponse_arr.length() > 0) {

                    System.out.println("cuberesponse_arr");
                    System.out.println(cuberesponse_arr);

                    List<String> facts = Util.retrieveDimensions(cubeurl + "/model");

                    String cube_table = "<table cellpadding='10'>";

                    cube_table += "<tr>";
                    for (String fact : facts) {

                        if (cuberesponse_arr.optJSONObject(0).has(fact)) {
                            cube_table += "<th>" + fact + "</th>";
                        }

                    }
                    cube_table += "<th>count</th>";
                    cube_table += "</tr>";

                    int[] barchartvalues = new int[cuberesponse_arr.length()];

                    for (int i = 0; i < cuberesponse_arr.length(); i++) {

                        String factsDescription = "";
                        cube_table += "<tr>";
                        for (String fact : facts) {

                            if (cuberesponse_arr.optJSONObject(i).has(fact)) {

                                String factid = cuberesponse_arr.getJSONObject(i).get(fact).toString();
                                System.out.println(fact + " : " + factid + "\n\n");
                                cube_table += "<td>" + factid + "</td>";
                                factsDescription += fact + ":" + factid + " ";
                            }

                        }

                        if (cuberesponse_arr.getJSONObject(i).has("count")) {
                            int countID = cuberesponse_arr.getJSONObject(i).getInt("count");
                            System.out.println("count : " + countID + "\n\n");
                            cube_table += "<td>" + countID + "</td>";
                            barchartvalues[i] = countID;
                            donutchartJSON.put(factsDescription, countID);
                        }

                        cube_table += "</tr>";

                    }

                    cube_table += "</table>";

                    objToReturn.put("cubetable", cube_table);
                    objToReturn.put("donutchart", donutchartJSON.toString());

                }
                objToReturn.put("totalcuberesponse", cuberesponse);
                objToReturn.put("cubeurl", urlToLoadAsLink);
                objToReturn.put("summary", Util.createSummaryTable(cuberesponse));
            }

            response.setContentType("text/x-json;charset=UTF-8");
            response.setHeader("Cache-Control", "no-cache");
            response.setStatus(HttpServletResponse.SC_OK);
            System.out.println("objToReturn" + objToReturn);
            response.getWriter().print(objToReturn.toString());
            response.getWriter().flush();
            response.getWriter().close();

        } catch (IOException ex) {
            Logger.getLogger(QueryPage.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//QueryPage

}//EoC


/*
 * Copyright (C) 2020 Emweb bv, Herent, Belgium.
 *
 * See the LICENSE file for terms of use.
 */
package eu.webtoolkit.jwt.chart;

import eu.webtoolkit.jwt.*;
import eu.webtoolkit.jwt.servlet.*;
import eu.webtoolkit.jwt.utils.*;
import java.io.*;
import java.lang.ref.*;
import java.util.*;
import java.util.regex.*;
import javax.servlet.*;
import javax.servlet.http.*;

/** Enumeration type that indicates a chart type for a cartesian chart. */
public enum ChartType {
  /** The X series are categories. */
  CategoryChart,
  /** The X series must be interpreted as numerical data. */
  ScatterPlot;

  /** Returns the numerical representation of this enum. */
  public int getValue() {
    return ordinal();
  }
}

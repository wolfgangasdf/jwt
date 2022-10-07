/*
 * Copyright (C) 2020 Emweb bv, Herent, Belgium.
 *
 * See the LICENSE file for terms of use.
 */
package eu.webtoolkit.jwt;

import eu.webtoolkit.jwt.chart.*;
import eu.webtoolkit.jwt.servlet.*;
import eu.webtoolkit.jwt.utils.*;
import java.io.*;
import java.lang.ref.*;
import java.time.*;
import java.util.*;
import java.util.regex.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A time validator.
 *
 * <p>
 *
 * @see WTimeEdit
 * @see WTime
 */
public class WTimeValidator extends WRegExpValidator {
  private static Logger logger = LoggerFactory.getLogger(WTimeValidator.class);

  /** Creates a new {@link WTimeValidator}. */
  public WTimeValidator() {
    super();
    this.formats_ = new ArrayList<String>();
    this.bottom_ = null;
    this.top_ = null;
    this.tooEarlyText_ = new WString();
    this.tooLateText_ = new WString();
    this.notATimeText_ = new WString();
    this.setFormat(WTime.getDefaultFormat());
  }
  /** Creates a new {@link WTimeValidator}. */
  public WTimeValidator(final String format) {
    super();
    this.formats_ = new ArrayList<String>();
    this.bottom_ = null;
    this.top_ = null;
    this.tooEarlyText_ = new WString();
    this.tooLateText_ = new WString();
    this.notATimeText_ = new WString();
    this.setFormat(format);
  }
  /**
   * Creates a new {@link WTimeValidator}.
   *
   * <p>The validator will accept only times within the indicated range <i>bottom</i> to <i>top</i>,
   * in the time formate <code>format</code>
   */
  public WTimeValidator(final String format, final WTime bottom, final WTime top) {
    super();
    this.formats_ = new ArrayList<String>();
    this.bottom_ = bottom;
    this.top_ = top;
    this.tooEarlyText_ = new WString();
    this.tooLateText_ = new WString();
    this.notATimeText_ = new WString();
    this.setFormat(format);
  }
  /** Sets the validator format. */
  public void setFormat(final String format) {
    if (this.formats_.isEmpty() || !this.formats_.get(0).equals(format)) {
      this.formats_.clear();
      this.formats_.add(format);
      this.repaint();
    }
  }
  /** Returns the validator current format. */
  public String getFormat() {
    return this.formats_.get(0);
  }
  /** Sets the time formats used to parse time strings. */
  public void setFormats(final List<String> formats) {
    Utils.copyList(formats, this.formats_);
    this.repaint();
  }
  /** Returns the time formats used to parse time strings. */
  public List<String> getFormats() {
    return this.formats_;
  }
  /**
   * Sets the lower limit of the valid time range.
   *
   * <p>The default is a null time constructed using WTime()
   */
  public void setBottom(final WTime bottom) {
    if (!(this.bottom_ == bottom || (this.bottom_ != null && this.bottom_.equals(bottom)))) {
      this.bottom_ = bottom;
      this.repaint();
    }
  }
  /** Returns the lower limit of the valid time range. */
  public WTime getBottom() {
    return this.bottom_;
  }
  /**
   * Sets the upper limit of the valid time range.
   *
   * <p>The default is a null time constructed using WTime()
   */
  public void setTop(final WTime top) {
    if (!(this.top_ == top || (this.top_ != null && this.top_.equals(top)))) {
      this.top_ = top;
      this.repaint();
    }
  }
  /** Returns the upper limit of the valid time range. */
  public WTime getTop() {
    return this.top_;
  }
  /** Sets the message to display when the input is not a time. */
  public void setInvalidNotATimeText(final CharSequence text) {
    this.notATimeText_ = WString.toWString(text);
  }
  /** Returns the message displayed when the input is not a time. */
  public WString getInvalidNotATimeText() {
    if (!(this.notATimeText_.length() == 0)) {
      WString s = this.notATimeText_;
      s.arg(this.formats_.get(0));
      return s;
    } else {
      return WString.tr("Wt.WTimeValidator.WrongFormat").arg(this.formats_.get(0));
    }
  }
  /** Sets the message to display when the time is earlier than bottom. */
  public void setInvalidTooEarlyText(final CharSequence text) {
    this.tooEarlyText_ = WString.toWString(text);
    this.repaint();
  }
  /** Returns the message displayed when time is too early. */
  public WString getInvalidTooEarlyText() {
    if (!(this.tooEarlyText_.length() == 0)) {
      WString s = this.tooEarlyText_;
      s.arg(this.bottom_.toString(this.formats_.get(0)))
          .arg(this.top_.toString(this.formats_.get(0)));
      return s;
    } else {
      if ((this.bottom_ == null)) {
        return new WString();
      } else {
        if ((this.top_ == null)) {
          return WString.tr("Wt.WTimeValidator.TimeTooEarly")
              .arg(this.bottom_.toString(this.formats_.get(0)));
        } else {
          return WString.tr("Wt.WTimeValidator.WrongTimeRange")
              .arg(this.bottom_.toString(this.formats_.get(0)))
              .arg(this.top_.toString(this.formats_.get(0)));
        }
      }
    }
  }
  /** Sets the message to display when the time is later than top. */
  public void setInvalidTooLateText(final CharSequence text) {
    this.tooLateText_ = WString.toWString(text);
    this.repaint();
  }
  /** Returns the message displayed when time is too late. */
  public WString getInvalidTooLateText() {
    if (!(this.tooLateText_.length() == 0)) {
      WString s = this.tooLateText_;
      s.arg(this.bottom_.toString(this.formats_.get(0)))
          .arg(this.top_.toString(this.formats_.get(0)));
      return s;
    } else {
      if ((this.top_ == null)) {
        return new WString();
      } else {
        if ((this.bottom_ == null)) {
          return WString.tr("Wt.WTimeValidator.TimeTooLate")
              .arg(this.top_.toString(this.formats_.get(0)));
        } else {
          return WString.tr("Wt.WTimeValidator.WrongTimeRange")
              .arg(this.bottom_.toString(this.formats_.get(0)))
              .arg(this.top_.toString(this.formats_.get(0)));
        }
      }
    }
  }
  /**
   * Validates the given input.
   *
   * <p>The input is considered valid only when it is blank for a non-mandatory field, or represents
   * a time in the given format, and within the valid range.
   */
  public WValidator.Result validate(final String input) {
    if (input.length() == 0) {
      return super.validate(input);
    }
    for (int i = 0; i < this.formats_.size(); i++) {
      try {
        WTime t = WTime.fromString(input, this.formats_.get(i));
        if ((t != null && t.isValid())) {
          if (!(this.bottom_ == null) && t.before(this.bottom_)) {
            return new WValidator.Result(ValidationState.Invalid, this.getInvalidTooEarlyText());
          }
          if (!(this.top_ == null) && t.after(this.top_)) {
            return new WValidator.Result(ValidationState.Invalid, this.getInvalidTooLateText());
          }
          return new WValidator.Result(ValidationState.Valid);
        }
      } catch (final RuntimeException e) {
        logger.warn(new StringWriter().append("validate(): ").append(e.toString()).toString());
      }
    }
    return new WValidator.Result(ValidationState.Invalid, this.getInvalidNotATimeText());
  }

  public String getJavaScriptValidate() {
    loadJavaScript(WApplication.getInstance());
    StringBuilder js = new StringBuilder();
    js.append("new Wt4_8_1.WTimeValidator(").append(this.isMandatory()).append(",[");
    for (int i = 0; i < this.formats_.size(); ++i) {
      WTime.RegExpInfo r = WTime.formatToRegExp(this.formats_.get(i));
      if (i != 0) {
        js.append(',');
      }
      js.append("{")
          .append("regexp:")
          .append(WWebWidget.jsStringLiteral(r.regexp))
          .append(',')
          .append("getHour:function(results){")
          .append(r.hourGetJS)
          .append(";},")
          .append("getMinutes:function(results){")
          .append(r.minuteGetJS)
          .append(";},")
          .append("getSeconds:function(results){")
          .append(r.secGetJS)
          .append(";},")
          .append("getMilliseconds:function(results){")
          .append(r.msecGetJS)
          .append(";},")
          .append("}");
    }
    js.append("],");
    if (!(this.bottom_ == null)) {
      js.append("new Date(0,0,0,")
          .append(this.bottom_.getHour())
          .append(",")
          .append(this.bottom_.getMinute())
          .append(",")
          .append(this.bottom_.getSecond())
          .append(",")
          .append(this.bottom_.getMsec())
          .append(")");
    } else {
      js.append("null");
    }
    js.append(',');
    if (!(this.top_ == null)) {
      js.append("new Date(0,0,0,")
          .append(this.top_.getHour())
          .append(",")
          .append(this.top_.getMinute())
          .append(",")
          .append(this.top_.getSecond())
          .append(",")
          .append(this.top_.getMsec())
          .append(")");
    } else {
      js.append("null");
    }
    js.append(',')
        .append(WString.toWString(this.getInvalidBlankText()).getJsStringLiteral())
        .append(',')
        .append(WString.toWString(this.getInvalidNotATimeText()).getJsStringLiteral())
        .append(',')
        .append(WString.toWString(this.getInvalidTooEarlyText()).getJsStringLiteral())
        .append(',')
        .append(WString.toWString(this.getInvalidTooLateText()).getJsStringLiteral())
        .append(");");
    return js.toString();
  }

  private List<String> formats_;
  private WTime bottom_;
  private WTime top_;
  private WString tooEarlyText_;
  private WString tooLateText_;
  private WString notATimeText_;

  private static void loadJavaScript(WApplication app) {
    app.loadJavaScript("js/WTimeValidator.js", wtjs1());
  }

  static WJavaScriptPreamble wtjs1() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptConstructor,
        "WTimeValidator",
        "(function(e,i,t,a,s,n,g,r){this.validate=function(l){if(0==l.length)return e?{valid:!1,message:s}:{valid:!0};for(var d,o=-1,u=-1,v=-1,m=-1,c=0;c<i.length;++c){var f=i[c];if(null!=(d=new RegExp(\"^\"+f.regexp+\"$\").exec(l))){o=f.getHour(d);l.toUpperCase().indexOf(\"P\")>-1&&o<12?o+=12:l.toUpperCase().indexOf(\"A\")>-1&&12==o&&(o=0);u=f.getMinutes(d);v=f.getSeconds(d);m=f.getMilliseconds(d);break}}if(null==d)return{valid:!1,message:n};if(o<0||o>23||u<0||u>59||v<0||v>59||m<0||m>999)return{valid:!1,message:n};var p=new Date(0,0,0,o,u,v,m);return p.getHours()!=o||p.getMinutes()!=u||p.getSeconds()!=v||p.getMilliseconds()!=m?{valid:!1,message:n}:t&&p.getTime()<t.getTime()?{valid:!1,message:g}:a&&p.getTime()>a.getTime()?{valid:!1,message:r}:{valid:!0}}})");
  }
}

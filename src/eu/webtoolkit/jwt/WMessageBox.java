/*
 * Copyright (C) 2009 Emweb bvba, Leuven, Belgium.
 *
 * See the LICENSE file for terms of use.
 */
package eu.webtoolkit.jwt;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.lang.ref.*;
import java.util.concurrent.locks.ReentrantLock;
import javax.servlet.http.*;
import javax.servlet.*;
import eu.webtoolkit.jwt.*;
import eu.webtoolkit.jwt.chart.*;
import eu.webtoolkit.jwt.utils.*;
import eu.webtoolkit.jwt.servlet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A standard dialog for confirmation or to get simple user input.
 * <p>
 * 
 * The messagebox shows a message in a dialog window, with a number of buttons.
 * These buttons may be standard buttons, or customized.
 * <p>
 * A messagebox is (usually) modal, and can be instantiated synchronously or
 * asynchronously.
 * <p>
 * When using a messagebox asynchronously, there is no API call that waits for
 * the messagebox to be processed. Instead, the usage is similar to
 * instantiating a {@link WDialog} (or any other widget). You need to connect to
 * the {@link WMessageBox#buttonClicked() buttonClicked()} signal to interpret
 * the result and delete the message box.
 * <p>
 * The synchronous use of a messagebox involves the use of the static
 * {@link WWidget#show() WWidget#show()} method, which blocks the current thread
 * until the user has processed the messabebox. Since this uses the
 * {@link WDialog#exec(WAnimation animation) WDialog#exec()}, it suffers from
 * the same scalability issues. See documentation of {@link WDialog} for more
 * details.
 * <p>
 * This will show a message box that looks like this:
 * <p>
 * <table border="0" align="center" cellspacing="3" cellpadding="3">
 * <tr>
 * <td><div align="center"> <img src="doc-files//WMessageBox-default-1.png"
 * alt="Example of a WMessageBox (default)">
 * <p>
 * <strong>Example of a WMessageBox (default)</strong>
 * </p>
 * </div></td>
 * <td><div align="center"> <img src="doc-files//WMessageBox-polished-1.png"
 * alt="Example of a WMessageBox (polished)">
 * <p>
 * <strong>Example of a WMessageBox (polished)</strong>
 * </p>
 * </div></td>
 * </tr>
 * </table>
 * <p>
 * <h3>i18n</h3>
 * <p>
 * The strings used in the {@link WMessageBox} buttons can be translated by
 * overriding the default values for the following localization keys:
 * <ul>
 * <li>Wt.WMessageBox.Abort: Abort</li>
 * <li>Wt.WMessageBox.Cancel: Cancel</li>
 * <li>Wt.WMessageBox.Ignore: Ignore</li>
 * <li>Wt.WMessageBox.No: No</li>
 * <li>Wt.WMessageBox.NoToAll: No to All</li>
 * <li>Wt.WMessageBox.Ok: Ok</li>
 * <li>Wt.WMessageBox.Retry: Retry</li>
 * <li>Wt.WMessageBox.Yes: Yes</li>
 * <li>Wt.WMessageBox.YesToAll: Yes to All</li>
 * </ul>
 */
public class WMessageBox extends WDialog {
	private static Logger logger = LoggerFactory.getLogger(WMessageBox.class);

	/**
	 * Creates an empty message box.
	 */
	public WMessageBox(WObject parent) {
		super(parent);
		this.buttons_ = new ArrayList<WMessageBox.Button>();
		this.icon_ = Icon.NoIcon;
		this.result_ = StandardButton.NoButton;
		this.buttonClicked_ = new Signal1<StandardButton>(this);
		this.defaultButton_ = null;
		this.escapeButton_ = null;
		this.create();
	}

	/**
	 * Creates an empty message box.
	 * <p>
	 * Calls {@link #WMessageBox(WObject parent) this((WObject)null)}
	 */
	public WMessageBox() {
		this((WObject) null);
	}

	/**
	 * Creates a message box with given caption, text, icon, and buttons.
	 */
	public WMessageBox(final CharSequence caption, final CharSequence text,
			Icon icon, EnumSet<StandardButton> buttons, WObject parent) {
		super(caption, parent);
		this.buttons_ = new ArrayList<WMessageBox.Button>();
		this.icon_ = Icon.NoIcon;
		this.buttonClicked_ = new Signal1<StandardButton>(this);
		this.defaultButton_ = null;
		this.escapeButton_ = null;
		this.create();
		this.setText(text);
		this.setIcon(icon);
		this.setButtons(buttons);
	}

	/**
	 * Creates a message box with given caption, text, icon, and buttons.
	 * <p>
	 * Calls
	 * {@link #WMessageBox(CharSequence caption, CharSequence text, Icon icon, EnumSet buttons, WObject parent)
	 * this(caption, text, icon, buttons, (WObject)null)}
	 */
	public WMessageBox(final CharSequence caption, final CharSequence text,
			Icon icon, EnumSet<StandardButton> buttons) {
		this(caption, text, icon, buttons, (WObject) null);
	}

	/**
	 * Sets the text for the message box.
	 */
	public void setText(final CharSequence text) {
		this.text_.setText(text);
	}

	/**
	 * Returns the message box text.
	 */
	public WString getText() {
		return this.text_.getText();
	}

	/**
	 * Returns the text widget.
	 * <p>
	 * This may be useful to customize the style or layout of the displayed
	 * text.
	 */
	public WText getTextWidget() {
		return this.text_;
	}

	/**
	 * Sets the icon.
	 */
	public void setIcon(Icon icon) {
		this.icon_ = icon;
		this.iconW_.toggleStyleClass("Wt-msgbox-icon",
				this.icon_ != Icon.NoIcon);
		this.text_
				.toggleStyleClass("Wt-msgbox-text", this.icon_ != Icon.NoIcon);
		this.iconW_.setSize(this.icon_ != Icon.NoIcon ? 2.5 : 1);
		switch (this.icon_) {
		case NoIcon:
			this.iconW_.setName("");
			break;
		case Information:
			this.iconW_.setName("info");
			break;
		case Warning:
			this.iconW_.setName("warning-sign");
			break;
		case Critical:
			this.iconW_.setName("exclamation");
			break;
		case Question:
			this.iconW_.setName("question");
		}
	}

	/**
	 * Returns the icon.
	 */
	public Icon getIcon() {
		return this.icon_;
	}

	/**
	 * Adds a custom button.
	 * <p>
	 * When the button is clicked, the associated result will be returned.
	 */
	public void addButton(WPushButton button, StandardButton result) {
		this.buttons_.add(new WMessageBox.Button());
		this.buttons_.get(this.buttons_.size() - 1).button = button;
		this.buttons_.get(this.buttons_.size() - 1).result = result;
		this.getFooter().addWidget(button);
		this.buttonMapper_.mapConnect(button.clicked(), result);
		if (button.isDefault()) {
			this.setDefaultButton(button);
		}
	}

	/**
	 * Adds a custom button with given text.
	 * <p>
	 * When the button is clicked, the associated result will be returned.
	 */
	public WPushButton addButton(final CharSequence text, StandardButton result) {
		WPushButton b = new WPushButton(text);
		this.addButton(b, result);
		return b;
	}

	/**
	 * Adds a standard button.
	 */
	public WPushButton addButton(StandardButton result) {
		return this.addButton(standardButtonText(result), result);
	}

	/**
	 * Sets standard buttons for the message box (<b>deprecated</b>).
	 * <p>
	 * 
	 * @deprecated this method has been renamed to
	 *             {@link WMessageBox#setStandardButtons(EnumSet buttons)
	 *             setStandardButtons()}.
	 */
	public void setButtons(EnumSet<StandardButton> buttons) {
		this.setStandardButtons(buttons);
	}

	/**
	 * Sets standard buttons for the message box (<b>deprecated</b>).
	 * <p>
	 * Calls {@link #setButtons(EnumSet buttons) setButtons(EnumSet.of(button,
	 * buttons))}
	 */
	public final void setButtons(StandardButton button,
			StandardButton... buttons) {
		setButtons(EnumSet.of(button, buttons));
	}

	/**
	 * Sets standard buttons for the message box.
	 */
	public void setStandardButtons(EnumSet<StandardButton> buttons) {
		this.buttons_.clear();
		this.getFooter().clear();
		this.defaultButton_ = this.escapeButton_ = null;
		for (int i = 0; i < 9; ++i) {
			if (!EnumUtils.mask(buttons, order_[i]).isEmpty()) {
				this.addButton(order_[i]);
			}
		}
	}

	/**
	 * Sets standard buttons for the message box.
	 * <p>
	 * Calls {@link #setStandardButtons(EnumSet buttons)
	 * setStandardButtons(EnumSet.of(button, buttons))}
	 */
	public final void setStandardButtons(StandardButton button,
			StandardButton... buttons) {
		setStandardButtons(EnumSet.of(button, buttons));
	}

	/**
	 * Returns the standard buttons.
	 * <p>
	 * 
	 * @see WMessageBox#setStandardButtons(EnumSet buttons)
	 * @see WMessageBox#addButton(WPushButton button, StandardButton result)
	 */
	public EnumSet<StandardButton> getStandardButtons() {
		EnumSet<StandardButton> result = EnumSet.noneOf(StandardButton.class);
		for (int i = 0; i < this.buttons_.size(); ++i) {
			result.add(this.buttons_.get(i).result);
		}
		return result;
	}

	/**
	 * Returns the buttons.
	 * <p>
	 * <p>
	 * <i><b>Note: </b>{@link WMessageBox#getButtons() getButtons()} returning
	 * WFlags&lt;StandardButton&gt; has been renamed to
	 * {@link WMessageBox#getStandardButtons() getStandardButtons()} in JWt
	 * 3.3.1 </i>
	 * </p>
	 */
	public List<WPushButton> getButtons() {
		List<WPushButton> result = new ArrayList<WPushButton>();
		for (int i = 0; i < this.buttons_.size(); ++i) {
			result.add(this.buttons_.get(i).button);
		}
		return result;
	}

	/**
	 * Returns the button widget for the given standard button.
	 * <p>
	 * Returns <code>null</code> if the button isn&apos;t in the message box.
	 * <p>
	 * This may be useful to customize the style or layout of the button.
	 */
	public WPushButton getButton(StandardButton b) {
		for (int i = 0; i < this.buttons_.size(); ++i) {
			if (this.buttons_.get(i).result == b) {
				return this.buttons_.get(i).button;
			}
		}
		return null;
	}

	/**
	 * Sets the button as the default button.
	 * <p>
	 * The default button is pressed when the user presses enter. Only one
	 * button can be the default button.
	 * <p>
	 * If no default button is set, JWt will take a button that is associated
	 * with a {@link StandardButton#Ok} or {@link StandardButton#Yes} result.
	 */
	public void setDefaultButton(WPushButton button) {
		if (this.defaultButton_ != null) {
			this.defaultButton_.setDefault(false);
		}
		this.defaultButton_ = button;
		if (this.defaultButton_ != null) {
			this.defaultButton_.setDefault(true);
		}
	}

	/**
	 * Sets the button as the default button.
	 * <p>
	 * The default button is pressed when the user presses enter. Only one
	 * button can be the default button.
	 * <p>
	 * The default value is 0 (no default button).
	 */
	public void setDefaultButton(StandardButton button) {
		WPushButton b = this.getButton(button);
		if (b != null) {
			this.setDefaultButton(b);
		}
	}

	/**
	 * Returns the default button.
	 * <p>
	 * 
	 * @see WMessageBox#setDefaultButton(WPushButton button)
	 */
	public WPushButton getDefaultButton() {
		return this.defaultButton_;
	}

	// public void setEscapeButton(WPushButton button) ;
	// public void setEscapeButton(StandardButton button) ;
	/**
	 * Returns the escape button.
	 * <p>
	 * 
	 * @see WMessageBox#setEscapeButton(WPushButton button)
	 */
	public WPushButton getEscapeButton() {
		return this.escapeButton_;
	}

	/**
	 * Returns the result of this message box.
	 * <p>
	 * This value is only defined after the dialog is finished.
	 */
	public StandardButton getButtonResult() {
		return this.result_;
	}

	/**
	 * Convenience method to show a message box, blocking the current thread.
	 * <p>
	 * Show a message box, blocking the current thread until the message box is
	 * closed, and return the result. The use of this method is not recommended
	 * since it uses {@link WDialog#exec(WAnimation animation) WDialog#exec()}.
	 * See documentation of {@link WDialog} for detailed information.
	 * <p>
	 * <i>This functionality is only available on Servlet 3.0 compatible servlet
	 * containers.</i>
	 */
	public static StandardButton show(final CharSequence caption,
			final CharSequence text, EnumSet<StandardButton> buttons,
			final WAnimation animation) {
		final WMessageBox box = new WMessageBox(caption, text,
				Icon.Information, buttons);
		box.buttonClicked().addListener(box,
				new Signal1.Listener<StandardButton>() {
					public void trigger(StandardButton e1) {
						box.accept();
					}
				});
		box.exec(animation);
		return box.getButtonResult();
	}

	/**
	 * Convenience method to show a message box, blocking the current thread.
	 * <p>
	 * Returns
	 * {@link #show(CharSequence caption, CharSequence text, EnumSet buttons, WAnimation animation)
	 * show(caption, text, buttons, new WAnimation())}
	 */
	public static final StandardButton show(final CharSequence caption,
			final CharSequence text, EnumSet<StandardButton> buttons) {
		return show(caption, text, buttons, new WAnimation());
	}

	/**
	 * Signal emitted when a button is clicked.
	 */
	public Signal1<StandardButton> buttonClicked() {
		return this.buttonClicked_;
	}

	public void setHidden(boolean hidden, final WAnimation animation) {
		if (!hidden) {
			if (!(this.defaultButton_ != null)) {
				for (int i = 0; i < this.buttons_.size(); ++i) {
					if (this.buttons_.get(i).result == StandardButton.Ok
							|| this.buttons_.get(i).result == StandardButton.Yes) {
						this.buttons_.get(i).button.setDefault(true);
						break;
					}
				}
			}
		}
		super.setHidden(hidden, animation);
	}

	static class Button {
		private static Logger logger = LoggerFactory.getLogger(Button.class);

		public WPushButton button;
		public StandardButton result;
	}

	private List<WMessageBox.Button> buttons_;
	private Icon icon_;
	private StandardButton result_;
	private Signal1<StandardButton> buttonClicked_;
	private WPushButton defaultButton_;
	private WPushButton escapeButton_;
	private WText text_;
	private WIcon iconW_;
	private WSignalMapper1<StandardButton> buttonMapper_;

	private void create() {
		this.iconW_ = new WIcon(this.getContents());
		this.text_ = new WText(this.getContents());
		this.getContents().addStyleClass("Wt-msgbox-body");
		this.buttonMapper_ = new WSignalMapper1<StandardButton>(this);
		this.buttonMapper_.mapped().addListener(this,
				new Signal1.Listener<StandardButton>() {
					public void trigger(StandardButton e1) {
						WMessageBox.this.onButtonClick(e1);
					}
				});
		this.rejectWhenEscapePressed();
		this.finished().addListener(this,
				new Signal1.Listener<WDialog.DialogCode>() {
					public void trigger(WDialog.DialogCode e1) {
						WMessageBox.this.onFinished();
					}
				});
	}

	private void onFinished() {
		if (this.getResult() == WDialog.DialogCode.Rejected) {
			if (this.escapeButton_ != null) {
				for (int i = 0; i < this.buttons_.size(); ++i) {
					if (this.buttons_.get(i).button == this.escapeButton_) {
						this.onButtonClick(this.buttons_.get(i).result);
						return;
					}
				}
			} else {
				if (this.buttons_.size() == 1) {
					this.onButtonClick(this.buttons_.get(0).result);
					return;
				} else {
					WPushButton b = this.getButton(StandardButton.Cancel);
					if (b != null) {
						this.onButtonClick(StandardButton.Cancel);
						return;
					}
					b = this.getButton(StandardButton.No);
					if (b != null) {
						this.onButtonClick(StandardButton.No);
						return;
					}
					this.onButtonClick(StandardButton.NoButton);
				}
			}
		}
	}

	private void onButtonClick(StandardButton b) {
		this.result_ = b;
		this.buttonClicked_.trigger(b);
	}

	// private void mappedButtonClick(StandardButton b) ;
	private static StandardButton[] order_ = { StandardButton.Ok,
			StandardButton.Yes, StandardButton.YesAll, StandardButton.Retry,
			StandardButton.No, StandardButton.NoAll, StandardButton.Abort,
			StandardButton.Ignore, StandardButton.Cancel };
	private static String[] buttonText_ = { "Wt.WMessageBox.Ok",
			"Wt.WMessageBox.Yes", "Wt.WMessageBox.YesToAll",
			"Wt.WMessageBox.Retry", "Wt.WMessageBox.No",
			"Wt.WMessageBox.NoToAll", "Wt.WMessageBox.Abort",
			"Wt.WMessageBox.Ignore", "Wt.WMessageBox.Cancel" };

	private static WString standardButtonText(StandardButton button) {
		for (int i = 0; i < 9; ++i) {
			if (order_[i] == button) {
				return tr(buttonText_[i]);
			}
		}
		return WString.Empty;
	}
}

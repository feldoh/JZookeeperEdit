package net.imagini.substrate;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import impl.org.controlsfx.i18n.Localization;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Necessary because Graal does not want to load resource bundles.
 * Therefore we provide a static hardcoded set of translations.
 * Mapped directly from <a href=https://github.com/controlsfx/controlsfx/blob/master/controlsfx/src/main/resources/controlsfx.properties />
 */
@TargetClass(Localization.class)
final class LocalizationSubstitution {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static ResourceBundle resourceBundle = new ResourceBundle() {
        private final Map<String, String> translations = new ConcurrentHashMap<>() {{
                //### Dialogs ###

                put("dlg.ok.button", "OK");
                put("dlg.cancel.button", "Cancel");
                put("dlg.yes.button", "Yes");
                put("dlg.no.button", "No");
                put("dlg.close.button", "Close");
                put("dlg.detail.button.more", "Show Details");
                put("dlg.detail.button.less", "Hide Details");

                //### Common Dialogs ###

                put("font.dlg.title", "Select font");
                put("font.dlg.header", "Select font");
                put("font.dlg.sample.text", "Sample");
                put("font.dlg.font.label", "Font");
                put("font.dlg.style.label", "Style");
                put("font.dlg.size.label", "Size");

                put("progress.dlg.title", "Progress");
                put("progress.dlg.header", "Progress");

                put("login.dlg.title", "Login");
                put("login.dlg.header", "Enter user name and password");
                put("login.dlg.user.caption", "User Name");
                put("login.dlg.pswd.caption", "Password");
                put("login.dlg.login.button", "Login");

                put("exception.dlg.title", "Exception Details");
                put("exception.dlg.header", "Exception Details");
                put("exception.dlg.label", "The exception stacktrace was:");
                put("exception.button.label", "Open Exception");

                //### Wizard ###

                put("wizard.next.button", "Next");
                put("wizard.previous.button", "Previous");

                //### Property Sheet ###

                put("bean.property.change.error.title", "Property Change Error");
                put("bean.property.change.error.header", "Change is not allowed");
                put("bean.property.category.basic", "Basic");
                put("bean.property.category.expert", "Expert");

                put("property.sheet.search.field.prompt", "Search");
                put("property.sheet.group.mode.byname", "By Name");
                put("property.sheet.group.mode.bycategory", "By Category");

                //### Spreadsheet View ###

                put("spreadsheet.view.menu.copy", "Copy");
                put("spreadsheet.view.menu.paste", "Paste");
                put("spreadsheet.view.menu.comment", "Comment cell");
                put("spreadsheet.view.menu.comment.top-left", "top left");
                put("spreadsheet.view.menu.comment.top-right", "top right");
                put("spreadsheet.view.menu.comment.bottom-right", "bottom right");
                put("spreadsheet.view.menu.comment.bottom-left", "bottom left");
                put("spreadsheet.column.menu.fix", "Freeze column");
                put("spreadsheet.column.menu.unfix", "Unfreeze column");
                put("spreadsheet.verticalheader.menu.fix", "Freeze row");
                put("spreadsheet.verticalheader.menu.unfix", "Unfreeze row");

                //### Status Bar ###
                put("statusbar.ok", "OK");

                //### List Selection View ###
                put("listSelectionView.header.source", "Available");
                put("listSelectionView.header.target", "Selected");

                //### PopOver ###
                put("popOver.default.content", "No Content");
                put("popOver.default.title", "Info");

                //### FilterPanel ###
                put("filterpanel.search.field", "Search...");
                put("filterpanel.apply.button", "APPLY");
                put("filterpanel.none.button", "NONE");
                put("filterpanel.all.button", "ALL");
                put("filterpanel.resetall.button", "RESET ALL");

                //### Notifications ###
                put("notifications.threshold.text", "You have received {0} notifications");

                //### TableView2 ###

                put("tableview2.column.menu.fixed", "Fixed column");
                put("tableview2.rowheader.menu.fixed", "Fixed row");


                //### Popup Filter ###

                put("popup.filter.case.sensitive.enable", "Enable case-sensitive comparision");
                put("popup.filter.case.sensitive.disable", "Disable case-sensitive comparision");


                //### Parser ###
                put("parser.text.error.start.operator", "Condition should start with an operator");
                put("parser.text.error.number.input", "Input must be a number");
                put("parser.text.error.string.start", "should start with a \"");
                put("parser.text.error.string.end", "should end with a \"");

                //## numbers
                //# see https://www.fileformat.info/info/unicode/block/mathematical_operators/utf8test.htm

                put("parser.text.operator.text.equals", "=");
                put("parser.text.operator.symbol.equals", "=");
                put("parser.text.operator.text.notequals", Character.toString('\u2260'));
                put("parser.text.operator.symbol.notequals", Character.toString('\u2260'));
                put("parser.text.operator.text.greaterthanequals", Character.toString('\u2265'));
                put("parser.text.operator.symbol.greaterthanequals", Character.toString('\u2265'));
                put("parser.text.operator.text.greaterthan", ">");
                put("parser.text.operator.symbol.greaterthan", ">");
                put("parser.text.operator.text.lessthanequals", Character.toString('\u2264'));
                put("parser.text.operator.symbol.lessthanequals", Character.toString('\u2264'));
                put("parser.text.operator.text.lessthan", "<");
                put("parser.text.operator.symbol.lessthan", "<");

                //## strings

                put("parser.text.operator.text.beginswith", "begins with");
                put("parser.text.operator.symbol.beginswith.sensitive", Character.toString('\u2291'));
                put("parser.text.operator.symbol.beginswith.insensitive", Character.toString('\u228f'));
                put("parser.text.operator.text.endswith", "ends with");
                put("parser.text.operator.symbol.endswith.sensitive", Character.toString('\u2292'));
                put("parser.text.operator.symbol.endswith.insensitive", Character.toString('\u2290'));
                put("parser.text.operator.text.contains", "contains");
                put("parser.text.operator.symbol.contains.sensitive", Character.toString('\u2287'));
                put("parser.text.operator.symbol.contains.insensitive", Character.toString('\u2283'));
                put("parser.text.operator.text.equalsto", "equals to");
                put("parser.text.operator.symbol.equalsto.sensitive", Character.toString('\u2261'));
                put("parser.text.operator.symbol.equalsto.insensitive", "=");
                put("parser.text.operator.text.notequalsto", "not equals to");
                put("parser.text.operator.symbol.notequalsto.sensitive", Character.toString('\u2262'));
                put("parser.text.operator.symbol.notequalsto.insensitive", Character.toString('\u2260'));

                put("parser.text.operator.symbol.default", Character.toString('\u25be'));

                //## aggregators

                put("parser.text.operator.text.and", "AND");
                put("parser.text.operator.text.or", "OR");

                //### South Filter ###
                put("southfilter.menubutton.checkbox.sensitive", "Case Sensitive");
                put("southfilter.menubutton.sensitive.enabled", "case sensitive");
                put("southfilter.menubutton.sensitive.disabled", "case insensitive");
            }};

        @Override
        protected Object handleGetObject(String key) {
            return translations.get(key);
        }

        @Override
        public Enumeration<String> getKeys() {
            return Collections.enumeration(translations.keySet());
        }
    };

    @Substitute
    private static synchronized ResourceBundle getLocaleBundle() {
        return resourceBundle;
    }
}

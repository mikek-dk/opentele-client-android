package dk.silverbullet.telemed.questionnaire.element;

import static dk.silverbullet.telemed.utils.Util.linkVariable;

import java.util.Map;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;

import com.google.gson.annotations.Expose;

import dk.silverbullet.telemed.questionnaire.R;
import dk.silverbullet.telemed.questionnaire.expression.Variable;
import dk.silverbullet.telemed.questionnaire.expression.VariableLinkFailedException;
import dk.silverbullet.telemed.questionnaire.node.IONode;
import dk.silverbullet.telemed.questionnaire.node.Node;
import dk.silverbullet.telemed.utils.Util;

@Data
@EqualsAndHashCode(callSuper = false)
public class CheckBoxElement extends Element {

    @SuppressWarnings("unused")
    private static final String TAG = Util.getTag(CheckBoxElement.class);

    @Setter
    @Expose
    private Variable<Boolean> outputVariable;

    @Expose
    private String text;

    @Setter(AccessLevel.PRIVATE)
    @Getter(AccessLevel.PRIVATE)
    private CheckBox checkBox;

    public CheckBoxElement(final IONode node) {
        super(node);
    }

    @Override
    public View getView() {
        if (null == checkBox) {
            Activity activity = getQuestionnaire().getActivity();
            LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            checkBox = (CheckBox) inflater.inflate(R.layout.checkbox, null);
            checkBox.setText(text);
        }

        checkBox.setChecked(outputVariable != null && outputVariable.getExpressionValue().getValue());
        return checkBox;
    }

    @Override
    public void leave() {
        if (null != checkBox && checkBox.isChecked())
            outputVariable.setValue(true);
        else
            outputVariable.setValue(false);

        InputMethodManager imm = (InputMethodManager) getQuestionnaire().getActivity().getApplicationContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(checkBox.getWindowToken(), 0);
    }

    @Override
    public void linkNodes(Map<String, Node> map) {
        // Done
    }

    @Override
    public void linkVariables(Map<String, Variable<?>> variablePool) throws VariableLinkFailedException {
        outputVariable = linkVariable(variablePool, outputVariable);
    }

    @Override
    public boolean validates() {
        return true;
    }
}

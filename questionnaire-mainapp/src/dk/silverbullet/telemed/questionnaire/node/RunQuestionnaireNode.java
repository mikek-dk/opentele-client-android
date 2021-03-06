package dk.silverbullet.telemed.questionnaire.node;

import java.util.Map;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import dk.silverbullet.telemed.deleteme.TestSkema;
import dk.silverbullet.telemed.questionnaire.Questionnaire;
import dk.silverbullet.telemed.questionnaire.expression.UnknownVariableException;
import dk.silverbullet.telemed.questionnaire.expression.Variable;
import dk.silverbullet.telemed.questionnaire.expression.VariableLinkFailedException;
import dk.silverbullet.telemed.questionnaire.output.OutputSkema;
import dk.silverbullet.telemed.questionnaire.skema.Skema;
import dk.silverbullet.telemed.rest.RetrieveSchemaTask;
import dk.silverbullet.telemed.rest.RetrieveTask;
import dk.silverbullet.telemed.rest.bean.QuestionnairListBean;
import dk.silverbullet.telemed.rest.listener.SkemaListener;
import dk.silverbullet.telemed.schedule.ReminderService;
import dk.silverbullet.telemed.utils.Util;

@Data
@EqualsAndHashCode(callSuper = false)
public class RunQuestionnaireNode extends Node implements SkemaListener {

    private static final String TAG = Util.getTag(RunQuestionnaireNode.class);

    private Node nextNode;
    @Getter(AccessLevel.PUBLIC)
    @Setter(AccessLevel.PUBLIC)
    private Variable<String> skemaName;

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private ProgressDialog dialog;

    private Skema skema;

    public RunQuestionnaireNode(Questionnaire questionnaire, String nodeName) {
        super(questionnaire, nodeName);
    }

    @Override
    public void enter() {
        questionnaire.cleanSkemaValuePool();

        dialog = ProgressDialog.show(questionnaire.getActivity(), "Henter skema", "Vent venligst...", true);

        try {
            if (skemaName.evaluate().startsWith("dk.silverbullet.telemed.deleteme")) {
                getLocal(skemaName.evaluate());
                setup();
            } else {
                String questionnaireJson = skemaName.evaluate();
                Context context = questionnaire.getActivity().getBaseContext();
                QuestionnairListBean deserializedQuestionnaire = deserializeQuestionnaire(questionnaireJson);

                getFromServer(questionnaireJson);
                ReminderService.clearRemindersForQuestionnaire(context, deserializedQuestionnaire.getName());
            }
        } catch (Exception e) {
            Log.e(TAG, "", e);
            ErrorNode errorNode = new ErrorNode(questionnaire, "errorNode");
            errorNode.setNextNode(nextNode);
            questionnaire.setCurrentNode(errorNode);
        }
    }

    public void setup() throws UnknownNodeException, VariableLinkFailedException {
        setupSkema();
        skema.getEndNodeNode().setNextNode(nextNode);
        questionnaire.setCurrentNode(skema.getStartNodeNode());
    }

    public void getFromServer(String questionnaireJson) {
        QuestionnairListBean bean = deserializeQuestionnaire(questionnaireJson);

        Variable<String> id = new Variable<String>(Util.VARIABLE_ID, String.class);
        id.setValue(bean.getId().toString());
        questionnaire.addVariable(id);

        OutputSkema outputSkema = new OutputSkema();
        outputSkema.setName(bean.getName());
        outputSkema.setVersion(bean.getVersion());
        outputSkema.setQuestionnaireId(bean.getId());

        questionnaire.setOutputSkema(outputSkema);

        RetrieveTask retrieveTask = new RetrieveSchemaTask(questionnaire, this);
        retrieveTask.execute();
    }

    public void getLocal(String skemaName) throws ClassNotFoundException, InstantiationException,
            IllegalAccessException, UnknownNodeException, VariableLinkFailedException {

        Class<?> c = null;
        c = Class.forName(skemaName);

        TestSkema ts = (TestSkema) c.newInstance();
        skema = ts.getSkema();
    }

    public void setupSkema() throws UnknownNodeException, VariableLinkFailedException {
        skema.link();
        skema.setQuestionnaire(questionnaire);
        for (Variable<?> output : skema.getOutput()) {
            questionnaire.addSkemaVariable(output);
        }

        for (Node node : skema.getNodes()) {
            node.linkVariables(questionnaire.getSkemaValuePool());
        }
        questionnaire.setStartNode(skema.getStartNodeNode());
    }

    @Override
    public void leave() {
        dialog.dismiss();
    }

    @Override
    public void linkNodes(Map<String, Node> map) {
        // Nothing to do
    }

    @Override
    public void linkVariables(Map<String, Variable<?>> map) throws UnknownVariableException {
        // Nothing to do
    }

    @Override
    public void setJson(String json) {
        try {
            skema = Util.getGson().fromJson(json, Skema.class);
            setup();
        } catch (Exception e) {
            ErrorNode errorNode = new ErrorNode(questionnaire, "errorNode");
            errorNode.setNextNode(nextNode);
            errorNode.setError(Util.stackTraceToString(e));
            Log.e(TAG, "Got exception", e);

            questionnaire.setCurrentNode(errorNode);
        }
    }

    @Override
    public void sendError() {
        ErrorNode errorNode = new ErrorNode(questionnaire, "errorNode");
        errorNode.setNextNode(nextNode);

        questionnaire.setCurrentNode(errorNode);
    }

    private QuestionnairListBean deserializeQuestionnaire(String skemaName) {
        return new Gson().fromJson(skemaName, QuestionnairListBean.class);
    }
}

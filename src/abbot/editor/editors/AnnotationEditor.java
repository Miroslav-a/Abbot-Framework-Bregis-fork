package abbot.editor.editors;

import java.awt.event.ActionEvent;
import javax.swing.*;
import abbot.*;
import abbot.i18n.Strings;
import abbot.script.Annotation;

/** A Annotation only has its description available for editing. */

public class AnnotationEditor extends StepEditor {

    private Annotation annotation;
    private JTextArea text;
    private JButton reposition;
    private JCheckBox userDismiss;
    private JComboBox relative;

    public AnnotationEditor(Annotation annotation) {
        super(annotation);
        this.annotation = annotation;
        text = addTextArea(Strings.get("AnnotationText"),
                           annotation.getText());
        relative = addComponentSelector(Strings.get("AnchorComponent"),
                                        annotation.getRelativeTo(),
                                        annotation.getResolver(), true);
        reposition = addButton(Strings.get("Reposition"));
        userDismiss = addCheckBox(Strings.get("UserDismiss"),
                                  annotation.getUserDismiss());
    }

    /** An editor component changed, respond to it by updating the step. */
    public void actionPerformed(ActionEvent ev) {
        Object src = ev.getSource();
        if (src == text) {
            annotation.setText(text.getText());
            fireStepChanged();
        }
        else if (src == relative) {
            annotation.setRelativeTo((String)relative.getSelectedItem());
            fireStepChanged();
        }
        else if (src == reposition) {
            try {
                annotation.showAnnotation();
                fireStepChanged();
            }
            catch(Exception e) {
                // FIXME show a dialog instead
                Log.warn(e);
            }
        }
        else if (src == userDismiss) {
            annotation.setUserDismiss(userDismiss.isSelected());
            if (annotation.isShowing())
                annotation.showAnnotation();
            fireStepChanged();
        }
        else {
            super.actionPerformed(ev);
        }
    }
}

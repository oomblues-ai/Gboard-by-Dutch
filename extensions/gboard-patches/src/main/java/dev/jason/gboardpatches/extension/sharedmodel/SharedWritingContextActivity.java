package dev.jason.gboardpatches.extension.sharedmodel;
import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.text.InputType;
/** Explicit caller-owned context; no incoming-chat or screenshot collection. */
public final class SharedWritingContextActivity extends Activity {
 @Override public void onCreate(Bundle saved){super.onCreate(saved);setTheme(android.R.style.Theme_Material_Light_NoActionBar);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(24,40,24,24);TextView title=new TextView(this);title.setText("Local AI context");title.setTextSize(24);body.addView(title);TextView note=new TextView(this);note.setText("Only the examples and message context you enter here are used. These fields stay in Gboard's private storage outside cloud backup. Incoming conversations are not collected.");body.addView(note);
  EditText style=entry(body,"Your style examples and preferences",SharedWritingPreferences.read(this,"style"));
  EditText reply=entry(body,"Explicit message to reply to",SharedWritingPreferences.read(this,"reply"));
  EditText instruction=entry(body,"Saved editing instruction",SharedWritingPreferences.read(this,"instruction"));
  Switch compose=new Switch(this);compose.setText("Shared Smart Compose (experimental)");compose.setChecked("true".equals(SharedWritingPreferences.read(this,"compose")));body.addView(compose);
  Switch grammar=new Switch(this);grammar.setText("Shared inline grammar (experimental)");grammar.setChecked("true".equals(SharedWritingPreferences.read(this,"grammar")));body.addView(grammar);
  Button save=new Button(this);save.setText("Save");save.setOnClickListener(v->{try{SharedWritingPreferences.write(this,style.getText().toString(),reply.getText().toString(),instruction.getText().toString(),compose.isChecked(),grammar.isChecked());SharedInlineSettings.updated(compose.isChecked(),grammar.isChecked());finish();}catch(Exception e){Toast.makeText(this,"Context is too long. Shorten it and try again.",Toast.LENGTH_LONG).show();}});body.addView(save);setContentView(new ScrollView(this){{addView(body);}});
 }
 private EditText entry(LinearLayout body,String hint,String value){EditText field=new EditText(this);field.setHint(hint);field.setText(value);field.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);field.setMinLines(2);body.addView(field);return field;}
}

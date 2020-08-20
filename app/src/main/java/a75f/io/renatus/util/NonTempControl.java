package a75f.io.renatus.util;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.common.base.Splitter;

import java.util.Iterator;

import a75f.io.renatus.R;

public class NonTempControl extends RelativeLayout {
        private boolean expand = false;
        private int equipType;
        private ImageView image = null;
        private ImageView imageViewExpanded = null;

        private TextView piInput = null;
        private TextView piOutput = null;
        private TextView piInputUnit = null;
        private TextView piOutputUnit = null;
        private TextView emTotal = null;
        private TextView emTotalUnit = null;
        private TextView emCurrent = null;
        private TextView emCurrentUnit = null;

        private String piInputText;
        private String piInputUnitText;
        private String piOutputText;
        private String piOutputUnitText;
        private String emTotalText;
        private String emTotalUnitText;
        private String emCurrentText;
        private String emCurrentUnitText;

        private RelativeLayout piValues;
        private RelativeLayout emValues;

    public NonTempControl(Context context) {
        super(context);
    }

    public NonTempControl(Context context, AttributeSet attrs) {
        super(context, attrs);
        initViews(context,attrs);
    }

    public NonTempControl(Context context, AttributeSet attrs, int defStyle) {
        this(context, attrs);
    }

    private void initViews(Context context, AttributeSet attrs) {

        LayoutInflater.from(context).inflate(R.layout.layout_imagecontrol, this);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.NonTempControl, 0, 0);

        try {
            expand = a.getBoolean(R.styleable.NonTempControl_expand,false);
            equipType = a.getInteger(R.styleable.NonTempControl_equipType,0);
            piInputText = a.getString(R.styleable.NonTempControl_piInputText);
            piInputUnitText = a.getString(R.styleable.NonTempControl_piInputUnitText);
            emTotalText = a.getString(R.styleable.NonTempControl_emTotalText);
            emTotalUnitText = a.getString(R.styleable.NonTempControl_emTotalUnitText);
            emCurrentText = a.getString(R.styleable.NonTempControl_emCurrentText);
            emCurrentUnitText = a.getString(R.styleable.NonTempControl_emCurrentUnitText);

            image = findViewById(R.id.imageView);
            imageViewExpanded = findViewById(R.id.imageViewExpanded);
            piInput = findViewById(R.id.textInput);
            piOutput = findViewById(R.id.textOutput);
            piInputUnit = findViewById(R.id.textInputCFM);
            piOutputUnit = findViewById(R.id.textOutputCFM);
            emTotal = findViewById(R.id.textEmTotal);
            emTotalUnit = findViewById(R.id.textTotalUnit);
            emCurrent = findViewById(R.id.textEmCurrent);
            emCurrentUnit = findViewById(R.id.textCurrentUnit);
            piValues = findViewById(R.id.piValues);
            emValues = findViewById(R.id.emValues);
        } finally {
            a.recycle();
        }

    }

    public  void setExpand(boolean expand)
    {
        this.expand = expand;
        if(image != null && imageViewExpanded != null && piValues != null && emValues != null) {
            if (expand) {
                image.setVisibility(View.GONE);
                imageViewExpanded.setVisibility(View.VISIBLE);
                if(equipType == 0){
                    piValues.setVisibility(GONE);
                    emValues.setVisibility(VISIBLE);
                    //imageViewExpanded.setPadding(10,16,16,16);
                }
                if(equipType == 1){
                    piValues.setVisibility(VISIBLE);
                    emValues.setVisibility(GONE);
                }
                if(equipType == 2){
                    piValues.setVisibility(GONE);
                    emValues.setVisibility(GONE);
                }
            } else if(!expand){
                image.setVisibility(View.VISIBLE);
                imageViewExpanded.setVisibility(View.GONE);
                piValues.setVisibility(GONE);
                emValues.setVisibility(GONE);
            }
        }
    }

    public Boolean isExpand() {
        return expand;
    }


    public int getEquipType() {
        return equipType;
    }

    public void setEquipType(int equipType) {
        this.equipType = equipType;
        if(piValues != null && emValues != null && image != null && imageViewExpanded != null) {
            if (equipType == 0) { //em
                piValues.setVisibility(GONE);
                emValues.setVisibility(VISIBLE);
                image.setPadding(10,10,10,10);
            }
            if (equipType == 1) { //pi
                piValues.setVisibility(VISIBLE);
                emValues.setVisibility(GONE);
            }
            if (equipType == 2) { //No Zones Paired

                piValues.setVisibility(GONE);
                emValues.setVisibility(GONE);
            }
        }
    }

    public  void setImage(int imageId)
    {
        if(image !=null)
        {
            image.setImageResource(imageId);
        }
    }

    public  void setImageViewExpanded(int imageId)
    {
        if(imageViewExpanded !=null)
        {
            imageViewExpanded.setImageResource(imageId);
        }
    }

    public String getPiInputText() {
        return piInputText;
    }

    public void setPiInputText(String piInputText) {
        this.piInputText = piInputText;
        if(piInput != null)
        {
            piInput.setText(piInputText);
        }
    }

    public String getPiInputUnitText() {
        return piInputUnitText;
    }

    public void setPiInputUnitText(String piInputUnitText) {
        this.piInputUnitText = piInputUnitText;
        if(piInputUnitText != null && piInputUnit != null){
            piInputUnit.setText(piInputUnitText);
        }
    }

    public String getPiOutputText() {
        return piOutputText;
    }

    public void setPiOutputText(String piOutputText) {
        this.piOutputText = piOutputText;
        if (piOutputText.length() > 4) {
            Iterable<String> piOutList = Splitter.fixedLength(4).split(piOutputText);
            Iterator iterator = piOutList.iterator();
            StringBuilder mBuilder = new StringBuilder();
            while (iterator.hasNext()) {
                mBuilder.append((String) iterator.next());
                if (iterator.hasNext()) {
                    mBuilder.append("\n");
                }
            }
            piOutputText = mBuilder.toString();
        }
        if (piOutputText != null && piOutput != null) {
            piOutput.setText(piOutputText);
        }
    }

    public String getPiOutputUnitText() {
        return piOutputUnitText;
    }

    public void setPiOutputUnitText(String piOutputUnitText) {
        this.piOutputUnitText = piOutputUnitText;
        if(piOutputUnitText != null && piOutputUnit != null){
            piOutputUnit.setText(piOutputUnitText);
        }
    }

    public String getEmTotalText() {
        return emTotalText;
    }

    public void setEmTotalText(String emTotalText) {
        this.emTotalText = emTotalText;
        if(emTotal != null){
            emTotal.setText(emTotalText);
        }
    }

    public String getEmTotalUnitText() {
        return emTotalUnitText;
    }

    public void setEmTotalUnitText(String emTotalUnitText) {
        this.emTotalUnitText = emTotalUnitText;
        if(emTotalUnit != null){
            emTotalUnit.setText(emTotalUnitText);
        }
    }

    public String getEmCurrentText() {
        return emCurrentText;
    }

    public void setEmCurrentText(String emCurrentText) {
        this.emCurrentText = emCurrentText;
        if(emCurrentText != null){
            emCurrent.setText(emCurrentText);
        }
    }

    public String getEmCurrentUnitText() {
        return emCurrentUnitText;
    }

    public void setEmCurrentUnitText(String emCurrentUnitText) {
        this.emCurrentUnitText = emCurrentUnitText;
        if(emCurrentUnit != null){
            emCurrentUnit.setText(emCurrentUnitText);
        }
    }
}

package a75f.io.renatus.registration;

import android.content.Context;
import android.graphics.PorterDuff;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import a75f.io.renatus.R;

public class VerticalTabAdapter extends BaseAdapter{
    private final int[] data;
    private final ListView listView;
    private OnItemClickListener listener;
    private Context mContext;
    private int currentSelected = 0;
    View menuLine;


    public VerticalTabAdapter(Context context, int[] data, ListView listView, OnItemClickListener listener, int position){
        this.mContext = context;
        this.data = data;
        this.listView = listView;
        this.listener = listener;
        this.currentSelected = position;
    }

    @Override
    public int getCount() {
        return data.length;
    }

    @Override
    public Object getItem(int i) {
        return data[i];
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        // Currently not using viewHolder pattern cause there aren't too many tabs in the demo project

        if(view == null){
            view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_verticaltabs, viewGroup, false);
        }

        ImageView menuImage = (ImageView)view.findViewById(R.id.Tab1);
        menuLine = (View)view.findViewById(R.id.viewLine);
        menuImage.setImageResource((int)getItem(i));

        if(i == currentSelected){
            setImageviewSelected(menuImage,i);
        }else{
            setImageviewUnSelected(menuImage);
        }
        if(i == data.length-1)
        {
            menuLine.setVisibility(View.GONE);
        }
        return view;
    }

    private void setImageviewSelected(ImageView tabIcon, int position) {
        Log.i("Tab","Position:"+position);
        if(position > 0) {
            for (int i = 0; i < data.length; i++) {
                View otherview = null;
                otherview = getViewByPosition(i);
                Log.i("Tab","View Position:"+i+" View:"+otherview);
                if(otherview != null) {
                    if (i < position) {
                        ((ImageView) otherview.findViewById(R.id.Tab1)).setColorFilter(mContext.getResources().getColor(android.R.color.black), PorterDuff.Mode.SRC_IN);
                    }
                    if (i > position) {
                        ((ImageView) otherview.findViewById(R.id.Tab1)).setColorFilter(mContext.getResources().getColor(android.R.color.darker_gray), PorterDuff.Mode.SRC_IN);
                    }
                }
                /*
                if(i == position) {
                    ((ImageView)otherview.findViewById(R.id.Tab1)).setColorFilter(mContext.getResources().getColor(R.color.orange_75f), PorterDuff.Mode.SRC_IN);
                }
                */
            }
        }
        tabIcon.setColorFilter(mContext.getResources().getColor(R.color.orange_75f), PorterDuff.Mode.SRC_IN);
    }

    private void setImageviewUnSelected(ImageView tabIcon){
        tabIcon.setColorFilter(mContext.getResources().getColor(android.R.color.darker_gray), PorterDuff.Mode.SRC_IN);
    }
    /**
     * Return item view at the given position or null if position is not visible.
     */
    public View getViewByPosition(int pos) {
        if(listView == null){
            return  null;
        }
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition ) {
            return null;
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
    }

    private void select(int position){
        if(currentSelected >= 0){
            deselect(currentSelected);
        }

        View targetView = getViewByPosition(position);
        if(targetView != null) {
            setImageviewSelected((ImageView)targetView.findViewById(R.id.Tab1),position);
        }

/*

        if(listener != null){
            listener.selectItem(position);
        }
*/

        currentSelected = position;

    }

    private void deselect(int position) {
        if(getViewByPosition(position) != null){
            View targetView = getViewByPosition(position);
            if(targetView != null) {
                //setTextViewToUnSelected((TextView)(targetView.findViewById(R.id.txt_tab_title)));
                setImageviewUnSelected((ImageView)targetView.findViewById(R.id.Tab1));
            }
        }
        currentSelected = -1;
    }


    // OnClick Events
/*

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        select(i);
    }
*/
/*


    public void OnItemClickListener(VerticalTabAdapter.OnItemClickListener listener){
        this.listener = listener;
    }
*/

    public void setCurrentSelected(int i) {
        select(i);
    }

    public interface OnItemClickListener{
        void selectItem(int position);
    }



}
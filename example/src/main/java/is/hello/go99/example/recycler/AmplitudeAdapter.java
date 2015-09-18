package is.hello.go99.example.recycler;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import is.hello.go99.animators.AnimatorContext;
import is.hello.go99.example.view.AmplitudeView;

public class AmplitudeAdapter extends RecyclerView.Adapter<AmplitudeAdapter.ViewHolder> {
    private final AnimatorContext animatorContext;
    private float[] amplitudes = {};

    public AmplitudeAdapter(@NonNull AnimatorContext animatorContext) {
        this.animatorContext = animatorContext;
    }


    //region Binding

    public void bindAmplitudes(@NonNull float[] amplitudes) {
        final int oldSize = this.amplitudes.length;

        this.amplitudes = amplitudes;

        final int newSize = amplitudes.length;

        if (oldSize > newSize) {
            notifyItemRangeRemoved(newSize, oldSize - newSize);
            notifyItemRangeChanged(0, newSize);
        } else if (newSize > oldSize) {
            notifyItemRangeInserted(oldSize, newSize - oldSize);
            notifyItemRangeChanged(0, oldSize);
        } else {
            notifyItemRangeChanged(0, newSize);
        }

    }

    public void clear() {
        final int oldSize = this.amplitudes.length;
        this.amplitudes = new float[0];
        notifyItemRangeRemoved(0, oldSize);
    }

    //endregion


    //region Rendering

    @Override
    public int getItemCount() {
        return amplitudes.length;
    }

    public float getAmplitude(int position) {
        return amplitudes[position];
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final AmplitudeView view = new AmplitudeView(parent.getContext());
        view.setAnimatorContext(animatorContext);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final float amplitude = getAmplitude(position);
        holder.amplitudeView.setAmplitude(amplitude);
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        holder.clearAnimation();
    }

    //endregion


    public class ViewHolder extends RecyclerView.ViewHolder {
        final AmplitudeView amplitudeView;

        ViewHolder(@NonNull AmplitudeView amplitudeView) {
            super(amplitudeView);

            this.amplitudeView = amplitudeView;
        }

        public void clearAnimation() {
            amplitudeView.clearAnimation();
        }

        public float getAmplitude() {
            return AmplitudeAdapter.this.getAmplitude(getAdapterPosition());
        }
    }
}

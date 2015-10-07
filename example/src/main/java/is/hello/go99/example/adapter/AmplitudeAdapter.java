/*
 * Copyright 2015 Hello, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package is.hello.go99.example.adapter;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.util.Collections;
import java.util.List;

import is.hello.go99.Anime;
import is.hello.go99.example.R;
import is.hello.go99.example.data.Amplitude;
import is.hello.go99.example.view.AmplitudeItemAnimator;
import is.hello.go99.example.view.AmplitudeView;

/**
 * Provides the views to render a series of amplitudes on {@link is.hello.go99.example.HomeActivity}.
 */
public class AmplitudeAdapter extends RecyclerView.Adapter<AmplitudeAdapter.ViewHolder> {
    private final int amplitudeHeightMin;
    private final int amplitudeHeightMax;

    private final OnClickListener onClickListener;
    private List<Amplitude> amplitudes = Collections.emptyList();

    public AmplitudeAdapter(@NonNull Resources resources,
                            @NonNull OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
        this.amplitudeHeightMin = resources.getDimensionPixelSize(R.dimen.view_amplitude_height_min);
        this.amplitudeHeightMax = resources.getDimensionPixelSize(R.dimen.view_amplitude_height_max);
    }


    //region Binding

    /**
     * Replaces the current contents of the adapter with a new collection of amplitudes,
     * sending differential item change notifications to allow the containing recycler
     * view to animate in the new values.
     *
     * @param amplitudes    The new amplitudes.
     */
    public void bindAmplitudes(@NonNull List<Amplitude> amplitudes) {
        final int oldSize = this.amplitudes.size();

        this.amplitudes = amplitudes;

        final int newSize = amplitudes.size();

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

    /**
     * Clears the contents of the adapter, sending a differential item change notification
     * to allow the containing recycler view to animate out the values.
     */
    public void clear() {
        final int oldSize = this.amplitudes.size();
        this.amplitudes = Collections.emptyList();
        notifyItemRangeRemoved(0, oldSize);
    }

    //endregion


    //region Rendering

    @Override
    public int getItemCount() {
        return amplitudes.size();
    }

    public Amplitude getAmplitude(int position) {
        return amplitudes.get(position);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final AmplitudeView view = new AmplitudeView(parent.getContext());
        view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT,
                                                           amplitudeHeightMin));
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Amplitude amplitude = getAmplitude(position);
        holder.bindAmplitude(amplitude);
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        holder.amplitudeView.clearAnimation();
    }

    //endregion


    /**
     * Encapsulates representation of a single {@link Amplitude} value.
     */
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public final AmplitudeView amplitudeView;
        private float amplitudeValue;

        ViewHolder(@NonNull AmplitudeView amplitudeView) {
            super(amplitudeView);

            this.amplitudeView = amplitudeView;
            amplitudeView.setOnClickListener(this);
        }

        /**
         * Binds a given amplitude object to the view holder, updating
         * the displayed value and height of the holder's amplitude view.
         *
         * @param amplitude The amplitude to bind.
         */
        void bindAmplitude(@NonNull Amplitude amplitude) {
            this.amplitudeValue = amplitude.value;
            amplitudeView.setAmplitude(amplitudeValue);

            final ViewGroup.LayoutParams layoutParams = amplitudeView.getLayoutParams();
            final float height = Anime.interpolateFloats(amplitude.height,
                                                         amplitudeHeightMin,
                                                         amplitudeHeightMax);
            layoutParams.height = Math.round(height);
            amplitudeView.requestLayout();
        }

        /**
         * Returns the value that the view holder should display when
         * the item added animations in the amplitudes fragment complete.
         *
         * @return The amplitude value this view holder should display.
         *
         * @see AmplitudeItemAnimator#runPendingAnimations() for usage.
         */
        public float getAmplitudeValue() {
            return amplitudeValue;
        }

        @Override
        public void onClick(View ignored) {
            if (getAdapterPosition() == RecyclerView.NO_POSITION ||
                    Anime.isAnimating(amplitudeView)) {
                return;
            }

            onClickListener.onItemClicked(this);
        }
    }

    public interface OnClickListener {
        void onItemClicked(@NonNull ViewHolder viewHolder);
    }
}

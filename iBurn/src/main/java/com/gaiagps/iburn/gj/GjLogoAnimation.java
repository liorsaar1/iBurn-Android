package com.gaiagps.iburn.gj;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

import com.gaiagps.iburn.R;

/**
 * Created by liorsaar on 2015-07-04
 */
public class GjLogoAnimation {

    private boolean isDone = false;

    public boolean isDone() {
        return isDone;
    }

    public void start(Activity activity) {
        final View parentView = activity.findViewById(R.id.logoAnimation);
        final View frontView = parentView.findViewById(R.id.logoAnimationFront);
        final View backView = parentView.findViewById(R.id.logoAnimationBack);

        frontView.setVisibility(View.VISIBLE);

        final ObjectAnimator backAnim = ObjectAnimator.ofFloat(backView, "alpha", 1.0f, 0.0f);
        backAnim.setInterpolator(new AccelerateInterpolator(0.5f));
        backAnim.setDuration(4 * 1000);
        backAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                parentView.setVisibility(View.GONE);
            }
        });

        ObjectAnimator frontAnim = ObjectAnimator.ofFloat(frontView, "alpha", 0.0f, 1.0f);
        frontAnim.setInterpolator(new AccelerateInterpolator(1f));
        frontAnim.setDuration(4 * 1000);
        frontAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                backAnim.start();
            }
        });
        // and go!
        frontAnim.start();
    }

}

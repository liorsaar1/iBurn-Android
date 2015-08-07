package com.gaiagps.iburn.gj;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;

import com.gaiagps.iburn.R;

/**
 * Created by liorsaar on 2015-07-04
 */
public class GjLogoAnimation {

    private static boolean isDone = false;

    public boolean isDone() {
        return isDone;
    }

    public void start(Activity activity, int animalId) {
        isDone = true;

        final View parentView = activity.findViewById(R.id.logoAnimation);
        final ImageView frontView = (ImageView) parentView.findViewById(R.id.logoAnimationFront);
        final ImageView backView = (ImageView) parentView.findViewById(R.id.logoAnimationBack);

        frontView.setImageResource(getFrontResId(animalId));
        backView.setImageResource(getBackResId(animalId));

        parentView.setVisibility(View.VISIBLE);
        frontView.setVisibility(View.VISIBLE);
        backView.setVisibility(View.VISIBLE);
        backView.setAlpha(1.0f);

        final ObjectAnimator backAnim = ObjectAnimator.ofFloat(backView, "alpha", 1.0f, 0.0f);
        backAnim.setInterpolator(new AccelerateInterpolator(0.3f));
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

    private int getFrontResId(int animalId) {
        switch (animalId) {
            case 1: return R.drawable.logo_animals_lion_front;
            case 2: return R.drawable.logo_animals_elephant_front;
            case 3: return R.drawable.logo_animals_tiger_front;
            case 4: return R.drawable.logo_animals_zebra_front;
            case 5: return R.drawable.logo_animals_rhino_front;
        }
        return R.drawable.logo_animals_tiger_front;
    }

    private int getBackResId(int animalId) {
        switch (animalId) {
            case 1: return R.drawable.logo_animals_lion_back;
            case 2: return R.drawable.logo_animals_elephant_back;
            case 3: return R.drawable.logo_animals_tiger_back;
            case 4: return R.drawable.logo_animals_zebra_back;
            case 5: return R.drawable.logo_animals_rhino_back;
        }
        return R.drawable.logo_animals_tiger_back;
    }

}

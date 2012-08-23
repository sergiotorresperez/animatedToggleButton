package org.garrapeta.commons;


import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ToggleButton;

/**
 * Toggle button which is able to show an animation from the moment the user
 * clicks on it (or programmatically checks / unchecks it) to the moment the
 * value is finally established.
 * 
 * When the button is showing its "transitioning" animation, the user can
 * "commit" (establish) the value, so the animation ends and the button reaches
 * a stable state, or "cancel" it, so the animation ends without altering the
 * checked value of the button.
 * 
 * If the user commits the change in the checked value, the button will show
 * another animation.
 * 
 * While the button is showing an animation (either the transitioning or the
 * commit one), it will report the state "android:state_active" to any
 * StateListDrawable implied in its graphical representation. This means that if
 * the background or color attributes in the XML declaration of the button
 * points to a StateListDrawable defined in an XML, the coder can use the state
 * "android:state_active" for selecting the resource to be chosen when the
 * button shows an animation.
 * 
 */
public class AnimatedToggleButton extends ToggleButton {

    // States the button can be in, regarding the transition of the change of value.
    protected enum TransitionState {
        STATE_STABLE,                       // The checked value of the button is stabilised. All normal ToggleButtons are always in this state
        STATE_TRANSITIONING,                 // The button is transitioning from one value to another. The transitioning animation is being shown.
        STATE_TRANSITIONING_COMMIT_PENDING,  // The user has commited the change in the checked value, but the operation has not been done yet.
        STATE_TRANSITIONING_CANCEL_PENDING}  // The user has cancelled the change in the checked value, but the operation has not been done yet.

    // Log tag
    private final static String TAG = AnimatedToggleButton.class.getSimpleName();
    
    private static final int[] TRANSITIONING_STATE_SET = {
        android.R.attr.state_active
    };

    // Key on the attribute in the XML to refer to the animation shown when the button is transitioning
    // from one checked value to another
    private static final String ATTR_TRANSITION_ANIMATION_ID = "transitionAnimation";
    // Key on the attribute in the XML to refer to the animation shown when the button eventually establishes its checked value
    private static final String ATTR_COMMIT_ANIMATION_ID = "commitAnimation";

    // Current state the button is in
    private TransitionState mTransitionState;
    // When in a transition, target checked value the button is transitioning to
    private boolean mTargetCheckedValue;

    // Animation shown when the button is transitioning from one checked value to another
    private Animation mTransitioningAnimation;
    // Animation shown when the button eventually establishes its checked value
    private Animation mCommitAnimation;
    

    /**
     * Constructor
     * @param context
     * @param attrs
     */
    public AnimatedToggleButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        gotoState(TransitionState.STATE_STABLE);
        initAnimations(attrs);
        
    }
    
    /**
     * Constructor
     * @param context
     * @param attrs
     * @param defStyle
     */
    public AnimatedToggleButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        gotoState(TransitionState.STATE_STABLE);
        initAnimations(attrs);
    }

    // Initialises the animations, given their ids as attributes in the XML
    private void initAnimations(AttributeSet attrs) {
        initTransitioningAnimation(attrs);
        gotoState(TransitionState.STATE_STABLE);
        initCommitAnimation(attrs);
    }

    // Initialises the transitioning animation, given their its as an attribute in the XML
    private void initTransitioningAnimation(AttributeSet attrs) {

        int animationId = attrs.getAttributeResourceValue(null, ATTR_TRANSITION_ANIMATION_ID, 0);
        mTransitioningAnimation = AnimationUtils.loadAnimation(getContext(), animationId);

        // TODO: Currently, this animation is mandatory. Support a button that can work without this animation. This is NOT needed now.
        if (mTransitioningAnimation == null ) {
            throw new IllegalStateException("Could not load the transitioning animation");
        }

        if (mTransitioningAnimation.getRepeatCount() == Animation.INFINITE) {
            throw new IllegalStateException("The transitioning animation cannot be infinitely repeated");
        }

        mTransitioningAnimation.setAnimationListener(new TransitioningAnimationListener());
    }

    // Initialises the commit animation, given their its as an attribute in the XML
    private void initCommitAnimation(AttributeSet attrs) {
        int animationId = attrs.getAttributeResourceValue(null, ATTR_COMMIT_ANIMATION_ID, 0);
        mCommitAnimation = AnimationUtils.loadAnimation(getContext(), animationId);

        // TODO: Currently, this animation is mandatory. Support a button that can work without this animation.
        if (mCommitAnimation == null ) {
            throw new IllegalStateException("Could not load the commit animation");
        }

        if (mCommitAnimation.getRepeatCount() == Animation.INFINITE) {
            throw new IllegalStateException("The commit animation cannot be infinitely repeated");
        }
        
        mCommitAnimation.setAnimationListener(new CommitAnimationListener());
    }


     @Override
    public boolean performClick() {
        if (TransitionState.STATE_STABLE != mTransitionState) {
            // If the button is transitioning we just ignore the click.
            // This prevents double clicking issues.
            return true;
        }
        return super.performClick();
    }

    /**
     * @see {@link android.widget.CompoundButton}
     * @param checked
     *            checked value
     * @param transition
     *            whether or not the change of value should be done with a
     *            tansition. If false, this behaves exactly as a normal
     *            ToggleButton. If true, the change of value won't occur
     *            inmediatly, but by showing an animation. The coder can commit
     *            or cancel the change of value while the animation is ongoing.
     */
    public void setChecked(boolean checked, boolean transition) {
        if (TransitionState.STATE_STABLE != mTransitionState) {
            Log.w(TAG, "Checked value changed when button was transitioning.");
        }
        if (transition) {
            if (isChecked() != checked) {
                gotoState(TransitionState.STATE_TRANSITIONING);
                mTargetCheckedValue = checked;
                startAnimation(mTransitioningAnimation);
            }
        } else {
            super.setChecked(checked);
        }
    }

    @Override
    public void setChecked(boolean checked) {
        // The default behaviour of setChecked, is doing the operation with a transition.
        setChecked(checked, true);
    }

    @Override
    public boolean isChecked() {
        if (mTransitionState == TransitionState.STATE_TRANSITIONING_COMMIT_PENDING) {
            // If the coder has commited the change but it is pending, we consider it to be applied in term of functionality.
            return mTargetCheckedValue;
        } else {
            return super.isChecked();
        }
    }
    
    @Override
    public void toggle() {
        // We need to everride this because the original CompoutButton uses a field to get the currently checked value. 
        // We have overriden the condition of being checked
        setChecked(!isChecked());
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (TransitionState.STATE_STABLE != mTransitionState)  {
            mergeDrawableStates(drawableState, TRANSITIONING_STATE_SET);
        }
        return drawableState;
    }

    /**
     * This method should be called by client code while the button is showing a
     * transition from one checked value to the opposite. Calling this will
     * terminate the transition and establish the checked value.
     * 
     * This method HAS to be called on the UI thread.
     * 
     * @throws IllegalArgumentException
     *             if this is called without an ongoing transition
     */
    public void commitCheckedChange() {
        if (TransitionState.STATE_TRANSITIONING != mTransitionState) {
            throw new IllegalStateException("For commiting the change, the button has to be transitioning");
        }
        gotoState(TransitionState.STATE_TRANSITIONING_COMMIT_PENDING);
    }

    /**
     * This method should be called by client code while the button is showing a
     * transition from one checked value to the opposite. Calling this will
     * terminate the transition and cancel the change in the checked value.
     * 
     * This method HAS to be called on the UI thread.
     * 
     * @throws IllegalArgumentException
     *             if this is called without an ongoing transition
     */
    public void cancelCheckedChange() {
        if (TransitionState.STATE_TRANSITIONING != mTransitionState) {
            throw new IllegalStateException("For cancelling the change, the button has to be transitioning");
        }
        gotoState(TransitionState.STATE_TRANSITIONING_CANCEL_PENDING);
    }

    private void gotoState(TransitionState state) {
        mTransitionState = state;
        refreshDrawableState();
    }


    /**
     * Listener of the transitioning animation.
     * 
     * If the user cancels / commits in the middle of the cycle of an animation,
     * we don't want to do the operation at that very moment: we want to wait
     * until the ongoing cycle finishes, so the transition between the
     * transitioning and commit animation is smooth and continous.
     * 
     * So, the animation uses this listener. When it finishes a cycle (repeats),
     * the request of commit or a cancellation is processed.
     */
    private class TransitioningAnimationListener implements AnimationListener {
        
        @Override
        public void onAnimationEnd(Animation animation) {
            // There is an issue in Android with the animations: http://code.google.com/p/android/issues/detail?id=13397
            // The "onAnimationRepeat" event never gets called. We cancel / commit everything in the onAnimationEnd event.
            // If we don't need cancels / commits pending, we just restart the animation to let it repeat.
            switch (mTransitionState) {
            case STATE_TRANSITIONING_COMMIT_PENDING:
                AnimatedToggleButton.super.setChecked(mTargetCheckedValue);
                startAnimation(mCommitAnimation);
                break;
                
            case STATE_TRANSITIONING_CANCEL_PENDING:
                gotoState(TransitionState.STATE_STABLE);
                break;

            case STATE_TRANSITIONING:
                animation.reset();
                animation.start();
                break;
            default:
                // this cannot happen
                break;
            }
        }
        
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    }

    /**
     * Listener of the commit animation.
     */
    private class CommitAnimationListener implements AnimationListener {
        @Override
        public void onAnimationEnd(Animation animation) {
            // when the commit animation ends, the button goes buck to stable 
            // and the change in the checked value finishes
            gotoState(TransitionState.STATE_STABLE);
        }

        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    }
}
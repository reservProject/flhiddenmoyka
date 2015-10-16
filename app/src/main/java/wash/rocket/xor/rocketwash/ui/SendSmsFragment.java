package wash.rocket.xor.rocketwash.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.octo.android.robospice.persistence.DurationInMillis;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import wash.rocket.xor.rocketwash.R;
import wash.rocket.xor.rocketwash.adapters.CountryAdapter;
import wash.rocket.xor.rocketwash.model.EmptyUserResult;
import wash.rocket.xor.rocketwash.model.PinResult;
import wash.rocket.xor.rocketwash.model.PostCarResult;
import wash.rocket.xor.rocketwash.model.Profile;
import wash.rocket.xor.rocketwash.model.ProfileResult;
import wash.rocket.xor.rocketwash.requests.CreateEmptyUserRequest;
import wash.rocket.xor.rocketwash.requests.PinRequest;
import wash.rocket.xor.rocketwash.requests.PostCarRequest;
import wash.rocket.xor.rocketwash.requests.ProfileSaveRequest;
import wash.rocket.xor.rocketwash.requests.SetPhoneRequest;
import wash.rocket.xor.rocketwash.util.Constants;
import wash.rocket.xor.rocketwash.util.Country;
import wash.rocket.xor.rocketwash.util.CountryMaster;
import wash.rocket.xor.rocketwash.util.util;

/**
 * A placeholder fragment containing a simple view.
 */
public class SendSmsFragment extends BaseFragment {

    public static final String TAG = "SendSmsFragment";
    private static final int MINUTES_WAIT = 1;

    private TextView txtCaption;
    private Button btnSendSMS;
    private EditText edPhone;

    private Timer timer;
    private long mLastTime = -1;
    private boolean waiting = false;

    private ProgressBar progressBar;

    private Spinner spinner;
    private int last_country_id = 0;
    private int cur_country_id = 0;

    private int mCarBrandId;
    private int mCarMoldelId;
    private String numCar;
    private String name;
    private Profile mProfile;

    private int mCount = 0;

    public SendSmsFragment() {

    }

    public static SendSmsFragment newInstance(int mCarBrandId, int mCarMoldelId, String numCar, String name) {
        SendSmsFragment fragment = new SendSmsFragment();
        Bundle args = new Bundle();

        args.putInt("mCarBrandId", mCarBrandId);
        args.putInt("mCarMoldelId", mCarMoldelId);
        args.putString("numCar", numCar);
        args.putString("name", name);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        if (getArguments() != null) {
            mCarBrandId = getArguments().getInt("mCarBrandId");
            mCarMoldelId = getArguments().getInt("mCarMoldelId");
            numCar = getArguments().getString("numCar");
            name = getArguments().getString("name");
            mProfile = new Profile();
            mProfile.setName(name);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_send_sms, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getView() == null)
            return;

        txtCaption = (TextView) getView().findViewById(R.id.txtCaption);
        btnSendSMS = (Button) getView().findViewById(R.id.btnSendSMS);
        edPhone = (EditText) getView().findViewById(R.id.edPhone);

        progressBar = (ProgressBar) getView().findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        btnSendSMS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (TextUtils.isEmpty(edPhone.getText().toString())) {
                    Toast.makeText(getActivity(), R.string.fragment_send_sms_emty_phone, Toast.LENGTH_LONG).show();
                    return;
                }

                hideKeyboard();
                if (waiting)
                    return;

                pref.saveLastUsedPhone(edPhone.getText().toString().trim());
                waiting = true;
                pref.setLastTimeClickSMS(System.currentTimeMillis());
                createTimer(pref.getLastTimeClickSMS());

                progressBar.setVisibility(View.VISIBLE);

                if (mProfile == null)
                    //getSpiceManager().execute(new PinRequest(pref.getLastUsedPhoneCode() + pref.getLastUsedPhone()), "pin", DurationInMillis.ONE_MINUTE, new PinRequestListener());
                    getSpiceManager().execute(new SetPhoneRequest(pref.getLastUsedPhoneCode() + pref.getLastUsedPhone(), pref.getSessionID()), "phone_set", DurationInMillis.ALWAYS_EXPIRED, new PhoneSetListener());
                else
                    getSpiceManager().execute(new CreateEmptyUserRequest(), "empty_user", DurationInMillis.ALWAYS_EXPIRED, new CreateEmptyUserListener());
            }
        });


        CountryMaster cm = CountryMaster.getInstance(getActivity());
        ArrayList<Country> countries = cm.getCountries();
        String countryIsoCode = cm.getDefaultCountryIso();
        Country country = cm.getCountryByIso(countryIsoCode);

        TelephonyManager manager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        String countryiso = manager.getSimCountryIso().toUpperCase();

        if (TextUtils.isEmpty(countryiso))
            countryiso = countryIsoCode;

        int c = 0;

        for (int i = 0; i < countries.size(); i++) {
            //System.out.println("countries.get(i).mCountryIso  = " + countries.get(i).mCountryIso);

            if (countries.get(i).mCountryIso.equals(countryiso)) {
                c = i;
                break;
            }
        }

        cur_country_id = c;
        spinner = (Spinner) getActivity().findViewById(R.id.spinner);
        CountryAdapter adapter = new CountryAdapter(getActivity(), (LayoutInflater) getActivity().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE), R.layout.view_country_list_item, countries);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                CountryMaster cm = CountryMaster.getInstance(getActivity());
                Country country = cm.getCountryByPosition(position);
                //txtPhoneCode.setText("+" + country.mDialPrefix);
                pref.setLastCountryId(position);
                pref.setLastUsedPhoneCode("+" + country.mDialPrefix);

                last_country_id = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // txtPhoneCode.setText("+");
            }
        });

        edPhone.setText(pref.getLastUsedPhone());

        if (savedInstanceState != null)
            last_country_id = savedInstanceState.getInt("last_country_id", 0);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (pref.getLastTimeClickSMS() > -1) {
            waiting = true;
            createTimer(pref.getLastTimeClickSMS());
        }


        final int idl = pref.getLastCountryId();
        //System.out.println("idl = " + idl);
        spinner.post(new Runnable() {
            @Override
            public void run() {
                int id = idl == 0 ? cur_country_id : idl;
                if (last_country_id > 0)
                    id = last_country_id;
                // System.out.println("id country = " + id);
                spinner.setSelection(id, false);
            }
        });

    }

    @TargetApi(3)
    private void hideKeyboard() {
        if (Build.VERSION.SDK_INT < 3) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(edPhone.getWindowToken(), 0);
        RelativeLayout rl = (RelativeLayout) getView().findViewById(R.id.main);
        rl.requestFocus();
    }


    private void stopCalculateTimer() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    private void createTimer(long lasttime) {
        stopCalculateTimer();
        mLastTime = lasttime;

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                mHandler.obtainMessage(1).sendToTarget();
            }

        }, 0, 1000);
    }

    TimerHandler mHandler = new TimerHandler(this);

    static class TimerHandler extends Handler {
        private final WeakReference<SendSmsFragment> mFragment;

        TimerHandler(SendSmsFragment fragment) {
            mFragment = new WeakReference<SendSmsFragment>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            SendSmsFragment fragment = mFragment.get();
            if (fragment != null) {
                fragment.handleTimer(msg);
            }
        }
    }

    private void handleTimer(Message msg) {
        long t = 60 * MINUTES_WAIT - ((System.currentTimeMillis() - mLastTime) / 1000);

        if (getActivity() == null)
            return;

        if (t <= 0) {
            waiting = false;
            stopCalculateTimer();
            pref.setLastTimeClick(-1);
            btnSendSMS.setText(R.string.button_next);
        } else
            btnSendSMS.setText(String.format("%s %s", getActivity().getString(R.string.fragment_login_btn_retry_pin_after), util.SecondsToMS(t)));
    }

    public final class PinRequestListener implements RequestListener<PinResult> {
        @Override
        public void onRequestFailure(SpiceException spiceException) {
            //Toast.makeText(getActivity(), R.string.request_pin_error, Toast.LENGTH_SHORT).show();
            showToastError(R.string.request_pin_phone_error);
            progressBar.setVisibility(View.GONE);
        }

        @Override
        public void onRequestSuccess(final PinResult result) {
            progressBar.setVisibility(View.GONE);
            Log.d("onRequestSuccess", result.getStatus() == null ? "null" : result.getStatus());

            if (Constants.SUCCESS.equals(result.getStatus())) {
                //  Toast.makeText(getActivity(), R.string.request_pin_success, Toast.LENGTH_SHORT).show();
                getActivity()
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.enter, R.anim.exit, R.anim.pop_enter, R.anim.pop_exit)
                        .add(R.id.container, new ConfirmationFragment(), ConfirmationFragment.TAG)
                        .addToBackStack(TAG).commit();

            } else {
                //Toast.makeText(getActivity(), R.string.request_pin_phone_error, Toast.LENGTH_SHORT).show();
                showToastError(R.string.request_pin_phone_error);
            }
        }
    }

    public final class PhoneSetListener implements RequestListener<ProfileResult> {
        @Override
        public void onRequestFailure(SpiceException spiceException) {
            // Toast.makeText(getActivity(), R.string.request_pin_error, Toast.LENGTH_SHORT).show();
            showToastError(R.string.request_pin_error);
            progressBar.setVisibility(View.GONE);
        }

        @Override
        public void onRequestSuccess(final ProfileResult result) {
            // progressBar.setVisibility(View.GONE);
            Log.d("PhoneSetListener", "onRequestSuccess = " + (result.getStatus() == null ? "null" : result.getStatus()));

            if (Constants.SUCCESS.equals(result.getStatus())) {

                if (TextUtils.isEmpty(result.getData().getPhone())) {
                    waiting = false;
                    stopCalculateTimer();
                    pref.setLastTimeClick(-1);
                    btnSendSMS.setText(R.string.button_next);
                    progressBar.setVisibility(View.GONE);

                    getActivity()
                            .getSupportFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(R.anim.enter, R.anim.exit, R.anim.pop_enter, R.anim.pop_exit)
                            .add(R.id.container, new LoginQuickFragment(), LoginQuickFragment.TAG)
                            .addToBackStack(TAG)
                            .commit();

                } else {
                    pref.setProfile(result.getData());
                    getSpiceManager().execute(new PinRequest(pref.getLastUsedPhoneCode() + pref.getLastUsedPhone()), "pin", DurationInMillis.ONE_MINUTE, new PinRequestListener());
                }

            } else {
                showToastError(R.string.request_pin_phone_error);
                waiting = false;
                stopCalculateTimer();
                pref.setLastTimeClick(-1);
                btnSendSMS.setText(R.string.button_next);
                progressBar.setVisibility(View.GONE);
            }
        }
    }

    public final class CreateEmptyUserListener implements RequestListener<EmptyUserResult> {
        @Override
        public void onRequestFailure(SpiceException spiceException) {
            //Toast.makeText(getActivity(), R.string.error_loading_data, Toast.LENGTH_SHORT).show();
            // progressBar.setVisibility(View.GONE);
        }

        @Override
        public void onRequestSuccess(final EmptyUserResult result) {
            // progressBar.setVisibility(View.GONE);
            //Toast.makeText(getActivity(), "login success", Toast.LENGTH_SHORT).show();
            Log.d("CreateEmptyUserListener", " result = " + result.toString());

            pref.setSessionID(result.getData().getSession_id());

            if (result != null)
                if (Constants.SUCCESS.equals(result.getStatus())) {
                    Log.d("CreateEmptyUserListener", "getSession_id = " + result.getData().getSession_id());
                    pref.setSessionID(result.getData().getSession_id());

                    getSpiceManager().execute(new ProfileSaveRequest(pref.getSessionID(), mProfile), "save_profile", DurationInMillis.ALWAYS_EXPIRED, new SaveProfileRequestListener());
                    getSpiceManager().execute(new PostCarRequest(mCarBrandId, mCarMoldelId, numCar, result.getData().getSession_id()), "create_car", DurationInMillis.ALWAYS_EXPIRED, new CreateCarListener());

                } else {

                }
        }
    }

    private class SaveProfileRequestListener implements RequestListener<ProfileResult> {
        @Override
        public void onRequestFailure(SpiceException spiceException) {

        }

        @Override
        public void onRequestSuccess(ProfileResult profileResult) {
            getSpiceManager().execute(new SetPhoneRequest(pref.getLastUsedPhoneCode() + pref.getLastUsedPhone(), pref.getSessionID()), "phone_set", DurationInMillis.ALWAYS_EXPIRED, new PhoneSetListener());
        }
    }

    private class CreateCarListener implements RequestListener<wash.rocket.xor.rocketwash.model.PostCarResult> {
        @Override
        public void onRequestFailure(SpiceException spiceException) {

        }

        @Override
        public void onRequestSuccess(PostCarResult postCarResult) {

        }
    }
}
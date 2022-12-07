package com.example.beaverairlines.home.childFragments

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.transition.AutoTransition
import android.transition.Transition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.example.beaverairlines.AuthViewModel
import com.example.beaverairlines.BookingViewModel
import com.example.beaverairlines.R
import com.example.beaverairlines.ViewModel
import com.example.beaverairlines.adapter.*
import com.example.beaverairlines.data.FlightOffer
import com.example.beaverairlines.data.Iata
import com.example.beaverairlines.data.User
import com.example.beaverairlines.data.model.Ad2Source
import com.example.beaverairlines.data.model.Booking
import com.example.beaverairlines.data.model.CabinClassSource
import com.example.beaverairlines.databinding.FragmentBookBinding
import com.example.beaverairlines.utils.BookInterface
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.book3_card.*
import kotlinx.android.synthetic.main.book3_card.view.*
import kotlinx.android.synthetic.main.fragment_book.*
import kotlinx.android.synthetic.main.fragment_book.view.*
import kotlinx.android.synthetic.main.passenger_input.view.*
import kotlinx.android.synthetic.main.pay_flights.view.*
import kotlinx.android.synthetic.main.pay_flights.view.iv_headerLogo1
import kotlinx.android.synthetic.main.pay_flights.view.iv_payFlight_backgroundBttnSheet
import kotlinx.android.synthetic.main.pay_flights.view.tv_Date1
import kotlinx.android.synthetic.main.pay_flights.view.tv_Date2
import kotlinx.android.synthetic.main.pay_flights.view.tv_Date3
import kotlinx.android.synthetic.main.pay_flights.view.tv_Date4
import kotlinx.android.synthetic.main.pay_flights.view.tv_Time1
import kotlinx.android.synthetic.main.pay_flights.view.tv_Time2
import kotlinx.android.synthetic.main.pay_flights.view.tv_Time3
import kotlinx.android.synthetic.main.pay_flights.view.tv_Time4
import kotlinx.android.synthetic.main.pay_flights.view.tv_adult1
import kotlinx.android.synthetic.main.pay_flights.view.tv_adult2
import kotlinx.android.synthetic.main.pay_flights.view.tv_cabinClass1
import kotlinx.android.synthetic.main.pay_flights.view.tv_cabinClass2
import kotlinx.android.synthetic.main.pay_flights.view.tv_flightNbr1
import kotlinx.android.synthetic.main.pay_flights.view.tv_flightNbr2
import kotlinx.android.synthetic.main.pay_flights.view.tv_price1
import kotlinx.android.synthetic.main.pay_flights.view.tv_price2
import kotlinx.android.synthetic.main.payment_procedure.*
import kotlinx.android.synthetic.main.payment_procedure.view.*
import kotlinx.android.synthetic.main.select_flights.view.*
import kotlinx.android.synthetic.main.select_flights.view.payCARDConstraint
import java.text.SimpleDateFormat
import java.util.*

//FRAGMENT HANDLING THE COMPLETE BOOKING PROCEDURE:

class BookFragment : Fragment(), BookInterface {

    private var firstLoad: Boolean = true
    private lateinit var binding: FragmentBookBinding

    private lateinit var departureDate: TextView
    private lateinit var returnDate: TextView
    private lateinit var adultPassengers: TextView
    private lateinit var infantPassengers: TextView
    private var adultCounter: Int = 1
    private var infantCounter: Int = 0

    private lateinit var iataArrayList: ArrayList<Iata>
    private lateinit var tempIataArrayList: ArrayList<Iata>
    private lateinit var paymentCardTransition: Transition

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var isReturnFlight = false
    private val flightViewModel: ViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()
    private val bookingViewModel: BookingViewModel by activityViewModels()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        firstLoad = true
        binding = FragmentBookBinding.inflate(inflater)
        return binding.root
    }




    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //ANIMATIONS TO BE USED:
        val fadeIN = AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.fade_in_fast)

        val fadeOUT = AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.fade_out_fast)

        val slideInRight = AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.slide_in_right)


        //ADVERTISING PART WITH AUTO SCROLL:
        val ad2Card = binding.ad2Card.AD2
        val advertising2 = Ad2Source().loadAd()
        val ad2Recycler = binding.ad2Card.BookFragmentRecycler
        val ad2TimeInSec : Long = 4000
        val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        ad2Recycler.setLayoutManager(linearLayoutManager)

        val ad2Adapter = Ad2Adapter(advertising2)
        ad2Recycler.adapter = ad2Adapter
        ad2Recycler.setHasFixedSize(true)

        val snapHelper3: SnapHelper = PagerSnapHelper()
        snapHelper3.attachToRecyclerView(ad2Recycler)

        val timer: Timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                if (linearLayoutManager.findLastCompletelyVisibleItemPosition() < ad2Adapter.getItemCount() - 1) {
                    linearLayoutManager.smoothScrollToPosition(ad2Recycler,
                        RecyclerView.State(),
                        linearLayoutManager.findLastCompletelyVisibleItemPosition() + 1)
                } else if (linearLayoutManager.findLastCompletelyVisibleItemPosition() === ad2Adapter.getItemCount() - 1) {
                    linearLayoutManager.smoothScrollToPosition(ad2Recycler, RecyclerView.State(), 0)
                }
            }
        }, 0, ad2TimeInSec)




        //PART 1 OF BOOKING PROCEDURE:
        flightViewModel.paymentCompleted.value = false

        val selectDepartureCity: AutoCompleteTextView = view.findViewById(R.id.tv_departCitySelect)
        val selectArrivalCity: AutoCompleteTextView = view.findViewById(R.id.tv_arriveCitySelect)
        val arrivalIata: TextView = view.findViewById(R.id.tv_IATAarrival)
        val departureIata: TextView = view.findViewById(R.id.tv_IATAdeparture)


        //OBSERVING IF THE BOOKING REQUEST WAS MADE FROM THE DASHBOARD FRAGMENT:
        flightViewModel.dBFlightSearchClicked.observe(
            viewLifecycleOwner
        ){
            if (it){
                selectDepartureCity.setText(flightViewModel.depCity2)
                departureIata.setText(flightViewModel.depIata2)
                selectArrivalCity.setText(flightViewModel.ariCity2)
                arrivalIata.setText(flightViewModel.ariIata2)


                Handler().postDelayed({
                TransitionManager.beginDelayedTransition(expandCard, AutoTransition())
                expandConstraint1.visibility = View.VISIBLE
                iv_planeIndicator.visibility = View.VISIBLE
                iv_planeIndicator.startAnimation(slideInRight)
                ib_arrow1.animate().setDuration(2).rotationBy(108f).start()
                ib_arrow1.visibility = View.GONE
                iv_redlineIndicator.visibility = View.VISIBLE
                iv_redlineIndicator.startAnimation(slideInRight)
                }, 1800)


                flightViewModel.dBFlightSearchClicked.value = false
            }
        }



        //CODE FOR PARSING IATA CODES MATCHING TO USER INPUT:
        tempIataArrayList = arrayListOf<Iata>()
        val iata = flightViewModel.iata
        tempIataArrayList.addAll(iata)


        context?.let { ctx ->
            val iataArrayAdapter2 = IataArrayAdapter(ctx, R.layout.iata_item, iata)
            selectDepartureCity.setAdapter(iataArrayAdapter2)
            selectDepartureCity.setOnItemClickListener { parent, _, position, _ ->
                val iata2 = iataArrayAdapter2.getItem(position) as Iata?
                selectDepartureCity.setText(iata2?.name)
                flightViewModel.depCity = (iata2?.name.toString())
                departureIata.setText(iata2?.iata)
            }
        }


        context?.let { ctx ->
            val iataArrayAdapter = IataArrayAdapter(ctx, R.layout.iata_item, iata)
            selectArrivalCity.setAdapter(iataArrayAdapter)
            selectArrivalCity.setOnItemClickListener { parent, _, position, _ ->
                val iata = iataArrayAdapter.getItem(position) as Iata?
                selectArrivalCity.setText(iata?.name)
                flightViewModel.ariCity = (iata?.name.toString())
                arrivalIata.setText(iata?.iata)
            }
        }




        //CODE FOR GETTING ACCESS TO CALENDER VIA DATEPICKER:
        departureDate = view.findViewById(R.id.tv_depDateSelect)
        returnDate = view.findViewById(R.id.tv_arriveDateSelect)
        val calender = Calendar.getInstance()

        val datePicker = DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
            calender.set(Calendar.YEAR, year)
            calender.set(Calendar.MONTH, month)
            calender.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updatelabel(calender)
        }

        val datePicker2 = DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
            calender.set(Calendar.YEAR, year)
            calender.set(Calendar.MONTH, month)
            calender.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updatelabel2(calender)
        }


        departureDate.setOnClickListener {
            DatePickerDialog(requireContext(),
                datePicker,
                calender.get(Calendar.YEAR),
                calender.get(Calendar.MONTH),
                calender.get(Calendar.DAY_OF_MONTH)).show()
        }


        returnDate.setOnClickListener {
            DatePickerDialog(requireContext(),
                datePicker2,
                calender.get(Calendar.YEAR),
                calender.get(Calendar.MONTH),
                calender.get(Calendar.DAY_OF_MONTH)).show()
        }



        //CODE FOR CABIN CLASS SELECTION:
        val cabinClasses = CabinClassSource().loadCabinClass()
        val ccAdapter = CabinClassAdapter(cabinClasses)
        binding.bigBookCard.rvCabinClass.adapter = ccAdapter

        val snapHelper: SnapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.bigBookCard.rvCabinClass)

        binding.bigBookCard.expandConstraint2.tv_cabinSelectedText.text =
            "Please select a Cabin Class"


        //RECEIVING CABIN CLASS UNIT FROM ADAPTER:
        ccAdapter.setOnItemClickListener {
            binding.bigBookCard.expandConstraint2.tv_cabinSelectedText.text = it.title
        }

        //VALUES AND CODE FOR PASSENGER COUNTER:
        adultPassengers = view.findViewById(R.id.tv_adultAmount)
        infantPassengers = view.findViewById(R.id.tv_infantAmount)
        adultPassengers.text = adultCounter.toString()
        infantPassengers.text = infantCounter.toString()


        binding.bigBookCard.expandConstraint3.bttn_adultPlus.setOnClickListener {
            adultCounter++
            adultPassengers.text = adultCounter.toString()
        }


        binding.bigBookCard.expandConstraint3.bttn_adultMinus.setOnClickListener {
            adultCounter--
            if (adultCounter < 0) {
                adultCounter = 0
            }
            adultPassengers.text = adultCounter.toString()
        }


        binding.bigBookCard.expandConstraint3.bttn_infantPlus.setOnClickListener {
            infantCounter++
            infantPassengers.text = infantCounter.toString()
        }


        binding.bigBookCard.expandConstraint3.bttn_infantMinus.setOnClickListener {
            infantCounter--
            if (infantCounter < 0) {
                infantCounter = 0
            }
            infantPassengers.text = infantCounter.toString()
        }




        //CODE FOR ANIMATING CLICK EVENTS:
        binding.bigBookCard.ibArrow1.setOnClickListener {
            if (expandConstraint1.visibility == View.GONE) {
                TransitionManager.beginDelayedTransition(expandCard, AutoTransition())
                expandConstraint1.visibility = View.VISIBLE
                iv_planeIndicator.visibility = View.VISIBLE
                iv_planeIndicator.startAnimation(slideInRight)
                ib_arrow1.animate().setDuration(2).rotationBy(108f).start()
                ib_arrow1.visibility = View.GONE
                iv_redlineIndicator.visibility = View.VISIBLE
                iv_redlineIndicator.startAnimation(slideInRight)

            } else {
                TransitionManager.beginDelayedTransition(expandCard, AutoTransition())
                expandConstraint1.visibility = View.GONE
                ib_arrow1.visibility = View.GONE
                ib_arrow1.animate().setDuration(2).rotationBy(108f).start()
            }
        }



        binding.bigBookCard.ibArrow2.setOnClickListener {
            if (expandConstraint2.visibility == View.GONE) {
                TransitionManager.beginDelayedTransition(expandCard, AutoTransition())
                expandConstraint2.visibility = View.VISIBLE
                ib_arrow2.animate().setDuration(2).rotationBy(108f).start()
                ib_arrow2.visibility = View.GONE
                iv_redlineIndicator2.visibility = View.VISIBLE
                iv_redlineIndicator2.startAnimation(slideInRight)
                iv_planeIndicator.startAnimation(slideInRight)
                iv_planeIndicator.translationX = iv_planeIndicator.translationX + 230

            } else {
                TransitionManager.beginDelayedTransition(expandCard, AutoTransition())
                expandConstraint2.visibility = View.GONE
                ib_arrow2.visibility = View.GONE
                ib_arrow2.animate().setDuration(2).rotationBy(108f).start()
            }
        }



        binding.bigBookCard.ibArrow3.setOnClickListener {
            if (expandConstraint3.visibility == View.GONE) {
                TransitionManager.beginDelayedTransition(expandCard, AutoTransition())
                expandConstraint3.visibility = View.VISIBLE
                ib_arrow3.animate().setDuration(2).rotationBy(108f).start()
                ib_arrow3.visibility = View.GONE
                iv_redlineIndicator3.visibility = View.VISIBLE
                iv_redlineIndicator3.startAnimation(slideInRight)
                iv_planeIndicator.startAnimation(slideInRight)
                iv_planeIndicator.translationX = iv_planeIndicator.translationX + 270
            } else {
                TransitionManager.beginDelayedTransition(expandCard, AutoTransition())
                expandConstraint3.visibility = View.GONE
                ib_arrow3.visibility = View.GONE
                ib_arrow3.animate().setDuration(2).rotationBy(108f).start()
            }
        }



        binding.bigBookCard.ibArrow4.setOnClickListener {
            if (expandConstraint4.visibility == View.GONE) {
                TransitionManager.beginDelayedTransition(expandCard, AutoTransition())
                expandConstraint4.visibility = View.VISIBLE
                //binding.constraintBigBookCard.bttn2.visibility = View.VISIBLE
                ib_arrow4.animate().setDuration(2).rotationBy(108f).start()
                ib_arrow4.visibility = View.GONE
                iv_redlineIndicator4.visibility = View.VISIBLE
                iv_redlineIndicator4.startAnimation(slideInRight)
                iv_planeIndicator.startAnimation(slideInRight)
                iv_planeIndicator.translationX = iv_planeIndicator.translationX + 230
                //binding.constraintBigBookCard.bttn2.startAnimation(slideInRight)
            } else {
                TransitionManager.beginDelayedTransition(expandCard, AutoTransition())
                expandConstraint4.visibility = View.GONE
                //binding.constraintBigBookCard.bttn2.visibility = View.GONE
                ib_arrow4.visibility = View.GONE
                ib_arrow4.animate().setDuration(2).rotationBy(108f).start()
            }
        }





        //CODE FOR OPENING THE SEARCH DIALOG INCL. ANIMATIONS:
        binding.constraintBigBookCard.bttn_searchFlights.setOnClickListener {

            flightViewModel.depIata = binding.bigBookCard.cvBookField.tv_IATAdeparture.text.toString()
            flightViewModel.ariIata = binding.bigBookCard.cvBookField.tv_IATAarrival.text.toString()
            binding.constraintBigBookCard.bttn2.visibility = View.VISIBLE


            val scaleBttnX = PropertyValuesHolder.ofFloat(View.SCALE_X, 10f)
            val scaleBttnY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 13f)
            val animatorBttn =
                ObjectAnimator.ofPropertyValuesHolder(binding.constraintBigBookCard.bttn2,
                    scaleBttnX, scaleBttnY)
            animatorBttn.duration = 200
            animatorBttn.start()



            //EARTH ANIMATION:
            val layoutIn = AnimationUtils.loadAnimation(
                requireContext(),
                R.anim.bttn_slide_in)

            binding.selectFlightsCard.loadingConstraint.visibility = View.VISIBLE
            binding.selectFlightsCard.loadingConstraint.startAnimation(layoutIn)

            val earthIn = AnimationUtils.loadAnimation(
                requireContext(),
                R.anim.bttn_slide_in)
            binding.selectFlightsCard.pbEarth.visibility = View.VISIBLE

            val animatorY = ObjectAnimator.ofFloat(binding.selectFlightsCard.pbEarth,
                View.TRANSLATION_Y,
                -1050f)
            animatorY.duration = 2000

            val animatorX =
                ObjectAnimator.ofFloat(binding.selectFlightsCard.pbEarth, View.TRANSLATION_X, -400f)
            animatorX.duration = 1000
            animatorX.repeatCount = 1
            animatorX.repeatMode = ObjectAnimator.REVERSE

            var toAlpha = 1f
            if (binding.selectFlightsCard.pbEarth.alpha == 0f) {
                toAlpha = 1f
            }

            val animatorF =
                ObjectAnimator.ofFloat(binding.selectFlightsCard.pbEarth, View.ALPHA, toAlpha)
            animatorF.duration = 500
            animatorF.start()


            val scaleSX = PropertyValuesHolder.ofFloat(View.SCALE_X, 10f)
            val scaleSY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 10f)
            val animatorS =
                ObjectAnimator.ofPropertyValuesHolder(binding.selectFlightsCard.pbEarth,
                    scaleSX,
                    scaleSY)
            animatorS.duration = 1000
            animatorS.start()


            val set = AnimatorSet()
            set.playTogether(animatorX, animatorY, animatorS)
            set.start()



            //DELAY FOR THE UPCOMING ANIMATION:
            Handler().postDelayed({
                val textIN = AnimationUtils.loadAnimation(
                    requireContext(),
                    R.anim.text_slide_in)
                textIN.duration = 1000

                binding.selectFlightsCard.tvLoadingFrom.visibility = View.VISIBLE
                binding.selectFlightsCard.tvLoadingFrom.startAnimation(textIN)
                binding.selectFlightsCard.tvLoadingDep.text = binding.bigBookCard.cvBookField.tv_IATAdeparture.text.toString()
                binding.selectFlightsCard.tvLoadingDep.visibility = View.VISIBLE
                binding.selectFlightsCard.tvLoadingDep.startAnimation(textIN)
                binding.selectFlightsCard.ivLoadingPlane.visibility = View.VISIBLE
                binding.selectFlightsCard.ivLoadingPlane.startAnimation(textIN)
                binding.selectFlightsCard.tvLoadingSearchText.visibility = View.VISIBLE
                binding.selectFlightsCard.tvLoadingSearchText.startAnimation(textIN)
            },800)


            //DELAY FOR THE UPCOMING ANIMATION:
            Handler().postDelayed({
                val textIN = AnimationUtils.loadAnimation(
                    requireContext(),
                    R.anim.text_slide_in)
                textIN.duration = 1000

                binding.selectFlightsCard.tvLoadingTo.visibility = View.VISIBLE
                binding.selectFlightsCard.tvLoadingTo.startAnimation(textIN)
                binding.selectFlightsCard.tvLoadingAri.text = binding.bigBookCard.cvBookField.tv_IATAarrival.text.toString()
                binding.selectFlightsCard.tvLoadingAri.visibility = View.VISIBLE
                binding.selectFlightsCard.tvLoadingAri.startAnimation(textIN)
                firstLoad = false
            },1000)


            //DELAY FOR THE UPCOMING ANIMATION:
            Handler().postDelayed({
                flightViewModel.status.observe(
                    viewLifecycleOwner,
                    androidx.lifecycle.Observer {

                        if (it) {
                            val beaverOut = AnimationUtils.loadAnimation(
                                requireContext(),
                                R.anim.fade_out)

                            binding.selectFlightsCard.tvLoadingSearchText.startAnimation(beaverOut)
                            binding.selectFlightsCard.tvLoadingSearchText.visibility = View.GONE

                            val resultsIN = AnimationUtils.loadAnimation(
                                requireContext(),
                                R.anim.text_slide_in_from_bttm)

                            binding.selectFlightsCard.cvResultsFirst.tv_loadingSearchText2.visibility = View.VISIBLE
                            binding.selectFlightsCard.cvResultsFirst.tv_loadingSearchText2.startAnimation(resultsIN)
                            binding.selectFlightsCard.cvResultsFirst.visibility = View.VISIBLE
                            binding.selectFlightsCard.cvResultsFirst.startAnimation(resultsIN)
                            binding.selectFlightsCard.cvResultsFirst.rv_firstFlightResultsList.visibility = View.VISIBLE
                            binding.selectFlightsCard.cvResultsFirst.rv_firstFlightResultsList.startAnimation(resultsIN)
                        }
                    }
                )
            }, 800)


            //OBSERVER FOR HANDLING PROPER UPCOMING ANIMATION:
            flightViewModel.started.observe(
                viewLifecycleOwner,
                androidx.lifecycle.Observer {
                    if (it) {
                        if(!firstLoad) {
                            val resultsOUT = AnimationUtils.loadAnimation(
                                requireContext(),
                                R.anim.bttnbar_slide_out
                            )

                            val textIN = AnimationUtils.loadAnimation(
                                requireContext(),
                                R.anim.text_slide_in
                            )


                            textIN.duration = 1000

                            binding.selectFlightsCard.loadingConstraint.tv_loadingDep.startAnimation(resultsOUT)
                            binding.selectFlightsCard.loadingConstraint.tv_loadingDep.visibility = View.INVISIBLE
                            binding.selectFlightsCard.loadingConstraint.tv_loadingAri.startAnimation(resultsOUT)
                            binding.selectFlightsCard.loadingConstraint.tv_loadingAri.visibility = View.INVISIBLE
                            binding.selectFlightsCard.loadingConstraint.tv_loadingSearchText.visibility = View.INVISIBLE
                            binding.selectFlightsCard.loadingConstraint.tv_loadingSearchText.startAnimation(resultsOUT)


                            if (isReturnFlight) {
                                binding.selectFlightsCard.loadingConstraint.tv_loadingDep.text = flightViewModel.ariIata
                                binding.selectFlightsCard.loadingConstraint.tv_loadingAri.text = flightViewModel.depIata


                            } else {
                                binding.selectFlightsCard.loadingConstraint.tv_loadingDep.text = flightViewModel.depIata
                                binding.selectFlightsCard.loadingConstraint.tv_loadingAri.text = flightViewModel.ariIata
                            }


                            binding.selectFlightsCard.loadingConstraint.tv_loadingFrom.visibility = View.VISIBLE
                            binding.selectFlightsCard.loadingConstraint.tv_loadingFrom.startAnimation(textIN)

                            binding.selectFlightsCard.loadingConstraint.tv_loadingDep.visibility = View.VISIBLE
                            binding.selectFlightsCard.loadingConstraint.tv_loadingDep.startAnimation(textIN)

                            binding.selectFlightsCard.loadingConstraint.iv_loadingPlane.visibility = View.VISIBLE
                            binding.selectFlightsCard.loadingConstraint.iv_loadingPlane.startAnimation(textIN)

                            binding.selectFlightsCard.loadingConstraint.tv_loadingTo.visibility = View.VISIBLE
                            binding.selectFlightsCard.loadingConstraint.tv_loadingTo.startAnimation(textIN)

                            binding.selectFlightsCard.loadingConstraint.tv_loadingAri.visibility = View.VISIBLE
                            binding.selectFlightsCard.loadingConstraint.tv_loadingAri.startAnimation(textIN)

                            binding.selectFlightsCard.loadingConstraint.tv_loadingSearchText.visibility = View.VISIBLE
                            binding.selectFlightsCard.loadingConstraint.tv_loadingSearchText.startAnimation(textIN)

                            binding.selectFlightsCard.cvResultsFirst.visibility = View.GONE
                            binding.selectFlightsCard.cvResultsFirst.startAnimation(resultsOUT)

                            binding.selectFlightsCard.cvResultsFirst.rv_firstFlightResultsList.visibility = View.GONE
                            binding.selectFlightsCard.cvResultsFirst.rv_firstFlightResultsList.startAnimation(resultsOUT)
                        }
                    }
                }
            )


            //OBSERVER FOR CHECKING IF FLIGHTS WERE PARSED:
            flightViewModel.offers.observe(
                viewLifecycleOwner,
                androidx.lifecycle.Observer {
                    if (it != null) {

                        if (isReturnFlight) {
                            val flightReturnAdapter: FlightReturnAdapter =
                                FlightReturnAdapter(it, flightViewModel.ariIata,flightViewModel.depIata,
                                    this)
                            binding.selectFlightsCard.cvResultsFirst.rv_firstFlightResultsList.adapter =
                                flightReturnAdapter

                        } else {
                            val flightSerachAdapter: FlightOfferAdapter =
                                FlightOfferAdapter(it, flightViewModel.depIata, flightViewModel.ariIata,
                                    this)
                            binding.selectFlightsCard.cvResultsFirst.rv_firstFlightResultsList.adapter =
                                flightSerachAdapter
                        }
                    }
                }
            )


            //METHOD FOR SHOWING FLIGHT RESULTS:
            openFlight()
        }
    }


    //METHODS FOR KEYBOARD BEHAVIOUR:
    private fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    private fun View.showKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }



    //METHOD TO FORMAT DATES:
    private fun updatelabel2(calender: Calendar) {
        val myFormat = "yyyy-MM-dd"
        val sdf = SimpleDateFormat(myFormat, Locale.GERMANY)
        returnDate.setText(sdf.format(calender.time))
    }

    private fun updatelabel(calender: Calendar) {
        val myFormat = "yyyy-MM-dd"
        val sdf = SimpleDateFormat(myFormat, Locale.GERMANY)
        departureDate.setText(sdf.format(calender.time))
    }



    //METHOD FOR SHOWING FLIGHT RESULTS:
    override fun openFlight() {

        isReturnFlight = false
        flightViewModel.resetAllValues()

        val resultsIN = AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.text_slide_in)

        flightViewModel.getFlights(
            binding.bigBookCard.cvBookField.tv_IATAdeparture.text.toString(),
            binding.bigBookCard.cvBookField.tv_IATAarrival.text.toString(),
            binding.bigBookCard.expandConstraint1.tv_depDateSelect.text.toString(),
            adultCounter
        )


        //HAND OVER OF DEPARTURE DATE TO VIEWMODEL FOR LATER USAGE:
        flightViewModel.departureDATE = binding.bigBookCard.expandConstraint1.tv_depDateSelect.text.toString()
    }



    //METHOD FOR SHOWING RETURN FLIGHT RESULTS:
    override fun openReturnFlight(flight: FlightOffer, bookingNbr: String) {
        flightViewModel.resetAllValues()
        isReturnFlight = true

        flightViewModel.flight1 = flight
        flightViewModel.bookingNbr1 = bookingNbr

        flightViewModel.getFlights(
            binding.bigBookCard.cvBookField.tv_IATAarrival.text.toString(),
            binding.bigBookCard.cvBookField.tv_IATAdeparture.text.toString(),
            binding.bigBookCard.expandConstraint1.tv_arriveDateSelect.text.toString(),
            adultCounter
        )

        flightViewModel.arrivalDATE = binding.bigBookCard.expandConstraint1.tv_arriveDateSelect.text.toString()
        flightViewModel.adultPassenger= adultCounter.toString()
    }



    //METHOD FOR PAYMENT PROCEDURE:
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    override fun openPayment(depIata: String, ariIata: String, returnFlight: FlightOffer, returnBookingNbr: String) {

        val flight1 = flightViewModel.flight1

        flightViewModel.flight2 = returnFlight
        flightViewModel.bookingNbr2 = returnBookingNbr

        binding.payCard.cvPayCARD.payCARDConstraint.tv_departIATA.text = ariIata
        binding.payCard.cvPayCARD.payCARDConstraint.tv_departCity.setText(flightViewModel.depCity.toString())
        binding.payCard.cvPayCARD.payCARDConstraint.tv_arrivalIATA1.text = depIata
        binding.payCard.cvPayCARD.payCARDConstraint.tv_arrivalCity1.text = flightViewModel.ariCity
        binding.payCard.cvPayCARD.payCARDConstraint.tv_arrivalIATA2.text = ariIata
        binding.payCard.cvPayCARD.payCARDConstraint.tv_arrivalCity2.text = flightViewModel.depCity
        binding.payCard.cvPayCARD.payCARDConstraint.cv_flight1.tv_flightNbr1.text = flightViewModel.bookingNbr1
        binding.payCard.cvPayCARD.payCARDConstraint.cv_flight1.tv_adult1.text = "${flightViewModel.adultPassenger} Adult"
        binding.payCard.cvPayCARD.payCARDConstraint.cv_flight1.tv_cabinClass1.text = "ECO Class"
        binding.payCard.cvPayCARD.payCARDConstraint.cv_flight1.tv_Date1.text = flightViewModel.departureDATE



        if (flight1 != null) {
            binding.payCard.cvPayCARD.payCARDConstraint.cv_flight1.tv_Time1.text = flight1.departureTime.dropLast(3)
        }

        binding.payCard.cvPayCARD.payCARDConstraint.cv_flight1.tv_Date2.text = flightViewModel.departureDATE

        if (flight1 != null) {
            binding.payCard.cvPayCARD.payCARDConstraint.cv_flight1.tv_Time2.text = flight1.arrivalTime.dropLast(3)
        }

        if (flight1 != null) {
            binding.payCard.cvPayCARD.payCARDConstraint.tv_price1.text = "EUR ${flight1.price}"
        }



        binding.payCard.cvPayCARD.payCARDConstraint.cv_flight2.tv_flightNbr2.text = returnBookingNbr
        binding.payCard.cvPayCARD.payCARDConstraint.cv_flight2.tv_adult2.text = "${flightViewModel.adultPassenger} Adult"
        binding.payCard.cvPayCARD.payCARDConstraint.cv_flight2.tv_cabinClass2.text = "ECO Class"
        binding.payCard.cvPayCARD.payCARDConstraint.cv_flight2.tv_Date3.text = flightViewModel.arrivalDATE
        binding.payCard.cvPayCARD.payCARDConstraint.cv_flight2.tv_Time3.text = returnFlight.departureTime.dropLast(3)
        binding.payCard.cvPayCARD.payCARDConstraint.cv_flight2.tv_Date4.text = flightViewModel.arrivalDATE
        binding.payCard.cvPayCARD.payCARDConstraint.cv_flight2.tv_Time4.text = returnFlight.arrivalTime.dropLast(3)
        binding.payCard.cvPayCARD.payCARDConstraint.tv_price2.text = "EUR ${returnFlight.price}"



        val resultsOUT = AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.bttnbar_slide_out)


        val resultsIN = AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.text_slide_in)


        val backgroundIN = AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.bttmsheet_slide_in)



        binding.selectFlightsCard.cvResultsFirst.startAnimation(resultsOUT)
        binding.selectFlightsCard.cvResultsFirst.visibility = View.GONE
        binding.selectFlightsCard.loadingConstraint.startAnimation(resultsOUT)
        binding.selectFlightsCard.loadingConstraint.visibility = View.GONE

        binding.payCard.ivCabinCrew.visibility = View.VISIBLE
        binding.payCard.ivCabinCrewCloud.visibility = View.VISIBLE
        binding.payCard.ivCabinCrew.startAnimation(resultsIN)
        binding.payCard.ivCabinCrewCloud.startAnimation(resultsIN)

        binding.payCard.cvPayCARD.visibility = View.VISIBLE
        binding.payCard.cvPayCARD.startAnimation(backgroundIN)
        binding.payCard.cvPayCARD.payCARDConstraint.iv_payFlight_backgroundBttnSheet.visibility = View.VISIBLE
        binding.payCard.cvPayCARD.payCARDConstraint.iv_payFlight_backgroundBttnSheet.startAnimation(resultsIN)



        //DELAY TO HANDLE PROPER ANIMATION ORDER:
        Handler().postDelayed({
            binding.payCard.cvPayCARD.payCARDConstraint.tv_checkYourFlight.visibility = View.VISIBLE
            binding.payCard.cvPayCARD.payCARDConstraint.tv_checkYourFlight.startAnimation(resultsIN)
            binding.payCard.cvPayCARD.payCARDConstraint.tv_checkYourFlight2.visibility = View.VISIBLE
            binding.payCard.cvPayCARD.payCARDConstraint.tv_checkYourFlight2.startAnimation(resultsIN)
            binding.payCard.cvPayCARD.payCARDConstraint.iv_headerLogo1.visibility = View.VISIBLE
            binding.payCard.cvPayCARD.payCARDConstraint.iv_headerLogo1.startAnimation(resultsIN)


            //ANIMATOR SET
            binding.payCard.cvPayCARD.payCARDConstraint.tv_departIATA.visibility = View.VISIBLE
            val cityAnimator1 = ObjectAnimator.ofFloat(
                binding.payCard.cvPayCARD.payCARDConstraint.tv_departIATA,
                View.ROTATION_X,
                0f, 360f)

            binding.payCard.cvPayCARD.payCARDConstraint.tv_departCity.visibility = View.VISIBLE
            val cityAnimator2 = ObjectAnimator.ofFloat(
                binding.payCard.cvPayCARD.payCARDConstraint.tv_departCity,
                View.ROTATION_X,
                0f, 360f)

            val set = AnimatorSet()
            set.playTogether(cityAnimator1, cityAnimator2)
            set.start()


            binding.payCard.cvPayCARD.payCARDConstraint.iv_dotLine1.startAnimation(resultsIN)
            binding.payCard.cvPayCARD.payCARDConstraint.iv_dotLine1.visibility = View.VISIBLE
            binding.payCard.cvPayCARD.payCARDConstraint.tv_arrivalIATA1.visibility = View.VISIBLE


            //ANIMATOR SET
            val cityAnimator3 = ObjectAnimator.ofFloat(
                binding.payCard.cvPayCARD.payCARDConstraint.tv_arrivalIATA1,
                View.ROTATION_X,
                0f, 360f)

            binding.payCard.cvPayCARD.payCARDConstraint.tv_arrivalCity1.visibility = View.VISIBLE
            val cityAnimator4 = ObjectAnimator.ofFloat(
                binding.payCard.cvPayCARD.payCARDConstraint.tv_arrivalCity1,
                View.ROTATION_X,
                0f, 360f)

            val set2 = AnimatorSet()
            set2.playTogether(cityAnimator3, cityAnimator4)
            set2.start()

            binding.payCard.cvPayCARD.payCARDConstraint.tv_price1.startAnimation(resultsIN)
            binding.payCard.cvPayCARD.payCARDConstraint.cv_flight1.startAnimation(resultsIN)
            binding.payCard.cvPayCARD.payCARDConstraint.iv_dotPlane1.startAnimation(resultsIN)
            binding.payCard.cvPayCARD.payCARDConstraint.cv_flight1.visibility = View.VISIBLE
            binding.payCard.cvPayCARD.payCARDConstraint.iv_dotPlane1.visibility = View.VISIBLE
            binding.payCard.cvPayCARD.payCARDConstraint.tv_price1.visibility = View.VISIBLE


        }, 1000)





        //DELAY TO HANDLE PROPER ANIMATION ORDER IN PAYMENT PROCEDURE:
        Handler().postDelayed({
            binding.payCard.cvPayCARD.payCARDConstraint.tv_arrivalIATA2.visibility = View.VISIBLE

            //ANIMATOR SET
            val cityAnimator5 = ObjectAnimator.ofFloat(
                binding.payCard.cvPayCARD.payCARDConstraint.tv_arrivalIATA2,
                View.ROTATION_X,
                0f, 360f)

            binding.payCard.cvPayCARD.payCARDConstraint.tv_arrivalCity2.visibility = View.VISIBLE
            val cityAnimator6 = ObjectAnimator.ofFloat(
                binding.payCard.cvPayCARD.payCARDConstraint.tv_arrivalCity2,
                View.ROTATION_X,
                0f, 360f)

            val set4 = AnimatorSet()
            set4.playTogether(cityAnimator5, cityAnimator6)
            set4.start()


            binding.payCard.cvPayCARD.payCARDConstraint.iv_dotLine2.startAnimation(resultsIN)
            binding.payCard.cvPayCARD.payCARDConstraint.iv_dotLine2.visibility = View.VISIBLE
            binding.payCard.cvPayCARD.payCARDConstraint.tv_price2.startAnimation(resultsIN)
            binding.payCard.cvPayCARD.payCARDConstraint.cv_flight2.startAnimation(resultsIN)
            binding.payCard.cvPayCARD.payCARDConstraint.iv_dotPlane2.startAnimation(resultsIN)
            binding.payCard.cvPayCARD.payCARDConstraint.cv_flight2.visibility = View.VISIBLE
            binding.payCard.cvPayCARD.payCARDConstraint.iv_dotPlane2.visibility = View.VISIBLE
            binding.payCard.cvPayCARD.payCARDConstraint.tv_price2.visibility = View.VISIBLE


            //DELAY WITHIN A DELAY FOR PROPER ANIMATION ORDER:
            Handler().postDelayed({
                binding.payCard.cvPayCARD.payCARDConstraint.bttn_proceedToPasDetails.startAnimation(resultsIN)
                binding.payCard.cvPayCARD.payCARDConstraint.bttn_proceedToPasDetails.visibility =View.VISIBLE
            }, 800)


        }, 1800)



        //CLICK LISTENER PROCESSING TO PASSENGER INPUT PART:
        binding.payCard.cvPayCARD.payCARDConstraint.bttn_proceedToPasDetails.setOnClickListener {

            val slideINFade = AnimationUtils.loadAnimation(
                requireContext(),
                R.anim.slide_in_right_fade)

            val payCard = binding.payCard.cvPayCARD

            binding.payCard.cvPayCARD.payCARDConstraint.tv_checkYourFlight.startAnimation(resultsOUT)
            binding.payCard.cvPayCARD.payCARDConstraint.tv_checkYourFlight.visibility = View.GONE
            binding.payCard.cvPayCARD.payCARDConstraint.tv_checkYourFlight2.startAnimation(resultsOUT)
            binding.payCard.cvPayCARD.payCARDConstraint.tv_checkYourFlight2.visibility = View.GONE
            binding.payCard.cvPayCARD.payCARDConstraint.iv_headerLogo1.startAnimation(resultsOUT)
            binding.payCard.cvPayCARD.payCARDConstraint.iv_headerLogo1.visibility = View.GONE
            payCard.startAnimation(resultsOUT)
            payCard.visibility = View.GONE


            val passengerInputCard = binding.passengerInputCard.cvPassengerCARD
            val pasBackground = binding.passengerInputCard.cvPassengerCARD.iv_backgroundPAS
            val pasHeader1 = binding.passengerInputCard.cvPassengerCARD.tv_header1PAS
            val pasHeader2 = binding.passengerInputCard.cvPassengerCARD.tv_header2PAS
            val pasHeader3 = binding.passengerInputCard.cvPassengerCARD.iv_headerLogoPAS
            val pasHeader4 = binding.passengerInputCard.cvPassengerCARD.iv_pasIcon
            val pasHeader5 = binding.passengerInputCard.cvPassengerCARD.tv_header3PAS
            val pasHeader6 = binding.passengerInputCard.cvPassengerCARD.tv_header4PAS
            val pasAdult = binding.passengerInputCard.cvPassengerCARD.tv_pasNo1
            val pasAdultName = binding.passengerInputCard.cvPassengerCARD.tv_pasNo1Name
            val pasArrow = binding.passengerInputCard.cvPassengerCARD.iv_pasArrow
            val passportCover = binding.passengerInputCard.cvPassengerCARD.iv_passportCover
            val passportCoverPasId = binding.passengerInputCard.cvPassengerCARD.tv_passportCoverPASid
            val bttnProceedToCheckout = binding.passengerInputCard.cvPassengerCARD.bttn_proceedToCheckout
            val savePassport = binding.passengerInputCard.cvPassengerCARD.bttn_savePassport
            val footer = binding.passengerInputCard.cvPassengerCARD.tv_footPAS

            val passportCard = binding.passengerInputCard.passportCARD
            val passportBackground = binding.passengerInputCard.passportCARD.iv_passportInside
            val country = binding.passengerInputCard.passportCARD.tv_pasCountry
            val passportNbr = binding.passengerInputCard.passportCARD.tv_pasPassportNbr1
            val surname = binding.passengerInputCard.passportCARD.tv_pasSurname
            val name = binding.passengerInputCard.passportCARD.tv_pasName
            val nationality = binding.passengerInputCard.passportCARD.tv_pasNationality
            val birthday = binding.passengerInputCard.passportCARD.tv_pasBirthday
            val birthCity = binding.passengerInputCard.passportCARD.tv_pasBirthCity
            val gender = binding.passengerInputCard.passportCARD.tv_pasGender
            var passportNbr2 = binding.passengerInputCard.passportCARD.tv_pasPassportNbr2
            val passportFooter = binding.passengerInputCard.passportCARD.tv_pasPassportFoot
            val bttnInputDone = binding.passengerInputCard.passportCARD.bttn_inputDone
            val visaStamp = binding.passengerInputCard.passportCARD.iv_visaStamp

            passengerInputCard.visibility = View.VISIBLE
            passengerInputCard.startAnimation(resultsIN)
            pasBackground.visibility = View.VISIBLE
            pasBackground.startAnimation(resultsIN)
            pasHeader1.visibility = View.VISIBLE
            pasHeader1.startAnimation(resultsIN)
            pasHeader2.visibility = View.VISIBLE
            pasHeader2.startAnimation(resultsIN)
            pasHeader3.visibility = View.VISIBLE
            pasHeader3.startAnimation(resultsIN)
            pasHeader4.visibility = View.VISIBLE
            pasHeader4.startAnimation(resultsIN)
            pasHeader5.visibility = View.VISIBLE
            pasHeader5.startAnimation(resultsIN)
            pasHeader6.visibility = View.VISIBLE
            pasHeader6.startAnimation(resultsIN)
            footer.visibility = View.VISIBLE
            footer.startAnimation(resultsIN)


            passportCover.visibility = View.VISIBLE
            passportCover.startAnimation(slideINFade)
            pasAdult.visibility = View.VISIBLE
            pasAdult.startAnimation(slideINFade)
            pasArrow.visibility = View.VISIBLE

            //ANIMATOR SET
            val arrowRotator = ObjectAnimator.ofFloat(
                pasArrow,
                View.ROTATION_Y,
                0f, 360f)
            arrowRotator.duration = 2000
            arrowRotator.start()


            //CLICK LISTENER TO OPEN PASSPORT CARD:
            pasAdult.setOnClickListener {

                val fadeOUT = AnimationUtils.loadAnimation(
                    requireContext(),
                    R.anim.fade_out_fast)


                passportCover.startAnimation(fadeOUT)
                passportCover.visibility = View.INVISIBLE
                passportCard.visibility = View.VISIBLE
                passportCard.startAnimation(resultsIN)
                passportBackground.visibility = View.VISIBLE
                passportBackground.startAnimation(resultsIN)
                passportNbr2.visibility = View.INVISIBLE
                passportNbr2.text = ""
                passportFooter.text = ""
                pasAdult.visibility = View.INVISIBLE
                pasArrow.visibility = View.INVISIBLE
                pasHeader6.visibility = View.INVISIBLE


                //DELAY FOR PROPER ANIMATION ORDER:
                Handler().postDelayed({
                    val passportCardAnim = ObjectAnimator.ofFloat(passportCard, View.TRANSLATION_Y,
                    -640f)
                    passportCardAnim.duration = 800
                    passportCardAnim.start()
                    it.showKeyboard()

                }, 800)


                //CLICK LISTENER WHEN INPUT IS DONE:
                bttnInputDone.setOnClickListener {
                    if (validatePasInput(name,
                            surname,
                            passportNbr,
                            country,
                            nationality,
                            birthday,
                            birthCity,
                            gender))
                    {

                        //ANIMATOR SET FOR PASSPORT CARD MOVEMENT:
                        val passportCardAnim2 = ObjectAnimator.ofFloat(passportCard, View.TRANSLATION_Y,
                            -640f,0f)

                        passportCardAnim2.duration = 200
                        passportCardAnim2.start()
                        passportNbr2.text = passportNbr.text.toString()
                        passportNbr2.visibility = View.VISIBLE
                        passportFooter.text = "***${passportNbr.text.toString()}***${surname.text.toString()}***${name.text.toString()}***" +
                                              "***********************************************************************"
                        passportFooter.visibility = View.VISIBLE


                        //DELAY FOR PROPER ANIMATION OF STAMP:
                        Handler().postDelayed({
                            val fadeINfast = AnimationUtils.loadAnimation(
                                requireContext(),
                                R.anim.fade_in_fast)

                            visaStamp.visibility = View.VISIBLE
                            visaStamp.startAnimation(fadeINfast)
                        }, 1000)



                        //ANIMATOR WITHIN DELAY SET FOR PASSPORT CARD MOVEMENT AND VALUES
                        Handler().postDelayed({
                            passportCard.startAnimation(resultsOUT)
                            passportBackground.startAnimation(resultsOUT)
                            passportBackground.visibility = View.GONE
                            passportCard.visibility = View.GONE
                            passportCover.startAnimation(resultsIN)
                            passportCover.visibility = View.VISIBLE
                            pasHeader6.visibility = View.VISIBLE
                            pasArrow.startAnimation(resultsOUT)
                            pasArrow.visibility = View.INVISIBLE
                            pasAdultName.text = "${name.text} ${surname.text}"
                            pasAdultName.startAnimation(resultsIN)
                            pasAdultName.visibility = View.VISIBLE
                            passportCoverPasId.text = "${name.text.take(1)} ${surname.text.take(1)} | ${passportNbr.text}"
                            passportCoverPasId.visibility = View.VISIBLE
                            pasAdult.visibility = View.VISIBLE
                            bttnProceedToCheckout.visibility = View.VISIBLE
                            bttnProceedToCheckout.startAnimation(resultsIN)
                            savePassport.visibility = View.VISIBLE
                            savePassport.startAnimation(resultsIN)
                        }, 3800)


                        //CHECKBOX TO SAVE PASSPORT INPUT TO FIREBASE AND MAKE IT ACCESSIBLE IN TRIPS FRAGMENT:
                        savePassport.setOnCheckedChangeListener { _, isChecked ->

                            db.collection("user").document(auth.currentUser!!.uid)
                                .get()
                                .addOnSuccessListener {

                                   val currentUserEmail = it.getString("email")
                                   val currentUserMileHigh = it.getString("mileHighClubNbr")


                            //UPDATING FIREBASE STORAGE WITH USER INPUT:
                            val updateUser = User(
                                fullName = "${name.text} ${surname.text}",
                                firstName = name.text.toString(),
                                lastName = surname.text.toString(),
                                birthDate = birthday.text.toString(),
                                birthPlace = birthCity.text.toString(),
                                gender = gender.text.toString(),
                                nationality = nationality.text.toString(),
                                country = country.text.toString(),
                                passportNbr = passportNbr.text.toString(),
                                email = currentUserEmail.toString(),
                                mileHighClubNbr = currentUserMileHigh.toString())

                            authViewModel.updateUser(updateUser)
                            flightViewModel.passportSaved.value = true
                        }
                    }


                        //CLICK LISTENER TO PROCEED TO PAYMENT:
                        bttnProceedToCheckout.setOnClickListener {
                            pasBackground.startAnimation(resultsOUT)
                            pasHeader1.startAnimation(resultsOUT)
                            pasHeader2.startAnimation(resultsOUT)
                            pasHeader3.startAnimation(resultsOUT)
                            pasHeader4.startAnimation(resultsOUT)
                            pasHeader5.startAnimation(resultsOUT)
                            pasHeader6.startAnimation(resultsOUT)
                            pasAdult.startAnimation(resultsOUT)
                            pasArrow.startAnimation(resultsOUT)
                            passportCover.startAnimation(resultsOUT)
                            bttnProceedToCheckout.startAnimation(resultsOUT)
                            savePassport.startAnimation(resultsOUT)
                            footer.startAnimation(resultsOUT)


                            pasBackground.visibility = View.GONE
                            pasHeader1.visibility = View.GONE
                            pasHeader2.visibility = View.GONE
                            pasHeader3.visibility = View.GONE
                            pasHeader4.visibility = View.GONE
                            pasHeader5.visibility = View.GONE
                            pasHeader6.visibility = View.GONE
                            pasAdult.visibility = View.GONE
                            pasArrow.visibility = View.GONE
                            passportCover.visibility = View.GONE
                            bttnProceedToCheckout.visibility = View.GONE
                            savePassport.visibility = View.GONE
                            footer.visibility = View.GONE


                            //METHOD TO HANDLE PAYMENT PROCEDURE:
                            openCheckout()
                        }

                    } else {
                        showErrorSnackBar("Please provide all fields to continue the booking reservation")
                    }
                }
            }
        }
    }



    //METHOD TO VALIDATE PASSPORT INPUT:
    private fun validatePasInput(
        name: EditText?,
        surname: EditText?,
        passportNbr: EditText?,
        country: EditText?,
        nationality: EditText?,
        birthday: EditText?,
        birthCity: EditText?,
        gender: EditText?,
    ): Boolean {

        return when {
            TextUtils.isEmpty(name?.text.toString()) -> {
                showErrorSnackBar("Please enter your first name")
                false
            }
            TextUtils.isEmpty(surname?.text.toString()) -> {
                showErrorSnackBar("Please enter your surname")
                false
            }
            TextUtils.isEmpty(passportNbr?.text.toString()) -> {
                showErrorSnackBar("Please enter your passport number ")
                false
            }
            TextUtils.isEmpty(country?.text.toString()) -> {
                showErrorSnackBar("Please enter your origin country")
                false
            }
            TextUtils.isEmpty(nationality?.text.toString()) -> {
                showErrorSnackBar("Please enter your nationality")
                false
            }
            TextUtils.isEmpty(birthday?.text.toString()) -> {
                showErrorSnackBar("Please enter your day of birth")
                false
            }
            TextUtils.isEmpty(birthCity?.text.toString()) -> {
                showErrorSnackBar("Please enter your city of birth")
                false
            }
            TextUtils.isEmpty(gender?.text.toString()) -> {
                showErrorSnackBar("Please enter your gender")
                false
            }
            else -> {
                true
            }
        }
    }



    //METHOD TO SHOW POSSIBLE ERROR MSG
    private fun showErrorSnackBar(message: String) {
            val snackbar = Snackbar.make(requireActivity().findViewById(android.R.id.content), message,
                Snackbar.LENGTH_LONG)

            val snackBarView = snackbar.view
            snackBarView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red_700))
            snackbar.show()
    }



    //METHOD FOR FINAL PAYMENT:
    @SuppressLint("SetTextI18n", "SuspiciousIndentation")
    private fun openCheckout() {

        val flightOne = flightViewModel.flight1
        val flightOneBookingNbr = flightViewModel.bookingNbr1
        val flightTwo = flightViewModel.flight2
        val flightTwoBookingNbr = flightViewModel.bookingNbr2


        val fadeIN = AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.fade_in)
            fadeIN.duration = 2000

        val resultsIN = AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.text_slide_in)

        val resultsOUT = AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.bttnbar_slide_out)


        //ASSIGNING VALUES:
        val surname = binding.passengerInputCard.passportCARD.tv_pasSurname
        val name = binding.passengerInputCard.passportCARD.tv_pasName
        val passportNbr = binding.passengerInputCard.passportCARD.tv_pasPassportNbr1

        val pasExpand = binding.paymentSummaryCard.cvPayCARD.expandLayoutPayment
        val paymentSummaryCard = binding.paymentSummaryCard
        val background = binding.paymentSummaryCard.cvPayCARD.iv_payFlight_backgroundBttnSheet
        val paymentCard = binding.paymentSummaryCard.cvPayCARD
        val header1 = binding.paymentSummaryCard.cvPayCARD.tv_header1
        val header2 = binding.paymentSummaryCard.cvPayCARD.tv_header2
        val headerLogo = binding.paymentSummaryCard.cvPayCARD.iv_headerLogo1

        val flight1From = binding.paymentSummaryCard.cvPayCARD.tv_fromCity1
        val flight1To = binding.paymentSummaryCard.cvPayCARD.tv_toCity1
        val flight1 = binding.paymentSummaryCard.cvPayCARD.cv_flight3
        val flight1Bg = binding.paymentSummaryCard.cvPayCARD.iv_background3
        val flightNbr1 = binding.paymentSummaryCard.cvPayCARD.tv_flightNbr3
        val adults1 = binding.paymentSummaryCard.cvPayCARD.tv_adult3
        val plane1One = binding.paymentSummaryCard.cvPayCARD.iv_PlaneICON3
        val date1One = binding.paymentSummaryCard.cvPayCARD.tv_date3
        val time1One = binding.paymentSummaryCard.cvPayCARD.tv_time3
        val plane1Two = binding.paymentSummaryCard.cvPayCARD.iv_PlaneICON3two
        val date1Two = binding.paymentSummaryCard.cvPayCARD.tv_date3two
        val time1Two = binding.paymentSummaryCard.cvPayCARD.tv_time3two
        val cabin1 = binding.paymentSummaryCard.cvPayCARD.tv_cabinClass3
        val flight1dep = binding.paymentSummaryCard.cvPayCARD.tv_fromCity1
        val arrow1 = binding.paymentSummaryCard.cvPayCARD.iv_arrowSmall3
        val flight1ari = binding.paymentSummaryCard.cvPayCARD.tv_toCity1
        val price1 = binding.paymentSummaryCard.cvPayCARD.tv_price1

        val flight2From = binding.paymentSummaryCard.cvPayCARD.tv_fromCity2
        val flight2To = binding.paymentSummaryCard.cvPayCARD.tv_toCity2
        val flight2 = binding.paymentSummaryCard.cvPayCARD.cv_flight4
        val flight2Bg = binding.paymentSummaryCard.cvPayCARD.iv_background4
        val flightNbr2 = binding.paymentSummaryCard.cvPayCARD.tv_flightNbr4
        val adults2 = binding.paymentSummaryCard.cvPayCARD.tv_adult4
        val plane2One = binding.paymentSummaryCard.cvPayCARD.iv_PlaneICON4
        val date2One = binding.paymentSummaryCard.cvPayCARD.tv_date4
        val time2One = binding.paymentSummaryCard.cvPayCARD.tv_time4
        val plane2Two = binding.paymentSummaryCard.cvPayCARD.iv_PlaneICON4two
        val date2Two = binding.paymentSummaryCard.cvPayCARD.tv_date4two
        val time2Two = binding.paymentSummaryCard.cvPayCARD.tv_time4two
        val cabin2 = binding.paymentSummaryCard.cvPayCARD.tv_cabinClass4
        val flight2dep = binding.paymentSummaryCard.cvPayCARD.tv_fromCity2
        val arrow2 = binding.paymentSummaryCard.cvPayCARD.iv_arrowSmall4
        val flight2ari = binding.paymentSummaryCard.cvPayCARD.tv_toCity2
        val price2 = binding.paymentSummaryCard.cvPayCARD.tv_price2

        val totalOverview = binding.paymentSummaryCard.cvPayCARD.cv_totalCost
        val line1 = binding.paymentSummaryCard.cvPayCARD.iv_fineLine1
        val line2 = binding.paymentSummaryCard.cvPayCARD.iv_fineLine2
        val charges1 = binding.paymentSummaryCard.cvPayCARD.tv_charge1
        val charges1Price = binding.paymentSummaryCard.cvPayCARD.tv_chargePrice1
        val charges2 = binding.paymentSummaryCard.cvPayCARD.tv_charge2
        val charges2Price = binding.paymentSummaryCard.cvPayCARD.tv_chargePrice2
        val grandTotal = binding.paymentSummaryCard.cvPayCARD.tv_grandTotal
        val grandTotalPrice = binding.paymentSummaryCard.cvPayCARD.tv_grandTotalPrice
        val frame = binding.paymentSummaryCard.cvPayCARD.iv_frame
        val proceedBttn = binding.paymentSummaryCard.cvPayCARD.bttn_proceedPayment2

        val payExpand1 = binding.paymentSummaryCard.cvPayCARD.expandConstraint1Payment
        val header3 = binding.paymentSummaryCard.cvPayCARD.expandConstraint1Payment.tv_header3
        val passengerDetailCard = binding.paymentSummaryCard.cvPayCARD.expandConstraint1Payment.cv_pasDetail
        val passengerDetailName = binding.paymentSummaryCard.cvPayCARD.expandConstraint1Payment.cv_pasDetail.tv_pasDetailName
        val passengerDetailArrow1 = binding.paymentSummaryCard.cvPayCARD.expandConstraint1Payment.cv_pasDetail.ib_pasDetailArrow
        val header4 = binding.paymentSummaryCard.cvPayCARD.expandConstraint1Payment.cv_pasDetail.tv_pasDetails2
        val passengerDetailPassportNbr = binding.paymentSummaryCard.cvPayCARD.expandConstraint1Payment.cv_pasDetail.tv_pasDetails3

        val payExpand2 = binding.paymentSummaryCard.cvPayCARD.expandConstraint2Payment
        val header5 = binding.paymentSummaryCard.cvPayCARD.expandConstraint2Payment.tv_header4
        val header6 = binding.paymentSummaryCard.cvPayCARD.expandConstraint2Payment.tv_header5
        val passengerDetailArrow2 = binding.paymentSummaryCard.cvPayCARD.expandConstraint2Payment.cv_pasDetail2.ib_pasDetailArrow2
        val passengerCCCard = binding.paymentSummaryCard.cvPayCARD.expandConstraint2Payment.cv_pasDetail2
        val ccBack = binding.paymentSummaryCard.cvPayCARD.expandConstraint2Payment.cv_pasDetail2.creditcardBack
        val ccBackBackground = binding.paymentSummaryCard.cvPayCARD.expandConstraint2Payment.cv_pasDetail2.creditcardBack.iv_ccBack
        val ccBackCardNbr = binding.paymentSummaryCard.cvPayCARD.expandConstraint2Payment.cv_pasDetail2.creditcardBack.tv_ccNbrBack
        val ccBackCvv = binding.paymentSummaryCard.cvPayCARD.expandConstraint2Payment.cv_pasDetail2.creditcardBack.tv_ccCvvBack
        val ccBackCvvLine = binding.paymentSummaryCard.cvPayCARD.expandConstraint2Payment.cv_pasDetail2.creditcardBack.iv_ccLine1
        val ccBackHolder = binding.paymentSummaryCard.cvPayCARD.expandConstraint2Payment.cv_pasDetail2.creditcardBack.tv_ccHolderBack
        val ccBackHolderLine = binding.paymentSummaryCard.cvPayCARD.expandConstraint2Payment.cv_pasDetail2.creditcardBack.iv_ccLine2
        val ccBackValid = binding.paymentSummaryCard.cvPayCARD.expandConstraint2Payment.cv_pasDetail2.creditcardBack.tv_ccValidBack
        val ccBackValidLine = binding.paymentSummaryCard.cvPayCARD.expandConstraint2Payment.cv_pasDetail2.creditcardBack.iv_ccLine3
        val ccFront = binding.paymentSummaryCard.cvPayCARD.expandConstraint2Payment.cv_pasDetail2.creditcardFront
        val ccFrontBackground = binding.paymentSummaryCard.cvPayCARD.expandConstraint2Payment.cv_pasDetail2.creditcardFront.iv_ccFront
        val ccFrontBackground2 = binding.paymentSummaryCard.cvPayCARD.expandConstraint2Payment.cv_pasDetail2.iv_ccFront2
        val ccFrontCardNbr = binding.paymentSummaryCard.cvPayCARD.expandConstraint2Payment.cv_pasDetail2.creditcardFront.tv_ccNbrFront
        val ccFrontHolder = binding.paymentSummaryCard.cvPayCARD.expandConstraint2Payment.cv_pasDetail2.creditcardFront.tv_ccHolderFront
        val ccFrontValid = binding.paymentSummaryCard.cvPayCARD.expandConstraint2Payment.cv_pasDetail2.creditcardFront.tv_ccValidFront
        val ccInputDone = binding.paymentSummaryCard.cvPayCARD.expandConstraint2Payment.cv_pasDetail2.bttn_inputCCdone
        val ccInputContinue = binding.paymentSummaryCard.cvPayCARD.expandConstraint2Payment.cv_pasDetail2.bttn_inputCCcontinue

        val payExpand3 = binding.paymentSummaryCard.cvPayCARD.expandConstraint3Payment
        val ticketReservationNbr = binding.paymentSummaryCard.cvPayCARD.expandConstraint3Payment.tv_ticketReservationNbr
        val footer = binding.paymentSummaryCard.cvPayCARD.expandConstraint3Payment.tv_footerPayment
        val payNowBttn = binding.paymentSummaryCard.cvPayCARD.expandConstraint3Payment.bttn_finalPay

        val paymentDoneConstraint = binding.paymentSummaryCard.cvPayCARD.paymentDONEConstraint
        val paymentDoneVideo =  binding.paymentSummaryCard.cvPayCARD.paymentDONEConstraint.paymentDoneVideo
        val goToBookingBttn = binding.paymentSummaryCard.cvPayCARD.paymentDONEConstraint.bttn_goToMyBookings


        flight1From.text = flightViewModel.depCity
        flight1To.text = flightViewModel.ariCity
        price1.text = "EUR ${flightOne?.price}"
        flightNbr1.text = flightOneBookingNbr
        adults1.text = "${flightViewModel.adultPassenger} Adult"
        date1One.text = flightViewModel.departureDATE
        time1One.text = flightOne?.departureTime
        date1Two.text = flightViewModel.departureDATE
        time1Two.text = flightOne?.arrivalTime
        cabin1.text = "ECO Class"

        flight2From.text = flightViewModel.ariCity
        flight2To.text = flightViewModel.depCity
        price2.text = "EUR ${flightTwo?.price}"
        flightNbr2.text = flightTwoBookingNbr
        adults2.text = "${flightViewModel.adultPassenger} Adult"
        date2One.text = flightViewModel.arrivalDATE
        time2One.text = flightTwo?.arrivalTime
        date2Two.text = flightViewModel.arrivalDATE
        time2Two.text = flightTwo?.departureTime
        cabin2.text = "ECO Class"


        //FUNCTION TO DO SIMPLE MATH:
        fun roundCharges (doubleNbr: Double): String {
            return "%.2f".format(doubleNbr)
        }


        //CODE TO CALCULATE FINAL PRICE:
        val charge1CalcA = (flightOne?.price)?.toDouble()
        val charge1CalcB = (flightTwo?.price)?.toDouble()
        val charge1CalcResult = (charge1CalcA!!.plus(charge1CalcB!!))
        charges1Price.text = "EUR ${roundCharges(charge1CalcResult)}"

        val charge2Calc = charge1CalcResult * 0.13
        charges2Price.text = "EUR ${roundCharges(charge2Calc)}"

        val totalCalc = charge1CalcResult + charge2Calc
        grandTotalPrice.text = "EUR ${roundCharges(totalCalc)}"



        //SHOWING "RECEIPT" LAYOUT OF PAYMENT:
        background.visibility = View.VISIBLE
        background.startAnimation(resultsIN)
        paymentCard.visibility = View.VISIBLE
        paymentCard.startAnimation(resultsIN)

        header1.visibility = View.VISIBLE
        header1.startAnimation(resultsIN)
        header2.visibility = View.VISIBLE
        header2.startAnimation(resultsIN)
        headerLogo.visibility = View.VISIBLE
        headerLogo.startAnimation(resultsIN)

        flight1.visibility = View.VISIBLE
        price1.visibility = View.VISIBLE
        flight2.visibility = View.VISIBLE
        price2.visibility = View.VISIBLE



        val flight1Animator = ObjectAnimator.ofFloat(
            flight1,
            View.ROTATION_Y,
            0f, 360f)

        val price1Animator = ObjectAnimator.ofFloat(
            price1,
            View.ROTATION_Y,
            0f, 360f)

        val flight1Set = AnimatorSet()
        flight1Set.playSequentially(flight1Animator, price1Animator)
        flight1Set.start()


        flight1dep.visibility = View.VISIBLE
        flight1dep.startAnimation(resultsIN)
        arrow1.visibility = View.VISIBLE
        arrow1.startAnimation(resultsIN)
        flight1ari.visibility = View.VISIBLE
        flight1ari.startAnimation(resultsIN)
        flight2.visibility = View.VISIBLE
        price2.visibility = View.VISIBLE



        val flight2Animator = ObjectAnimator.ofFloat(
            flight2,
            View.ROTATION_Y,
            0f, 360f)

        val price2Animator = ObjectAnimator.ofFloat(
            price2,
            View.ROTATION_Y,
            0f, 360f)

        val flight2Set = AnimatorSet()
        flight2Set.playSequentially(flight2Animator, price2Animator)
        flight2Set.start()


        flight2dep.visibility = View.VISIBLE
        flight2dep.startAnimation(resultsIN)
        arrow2.visibility = View.VISIBLE
        arrow2.startAnimation(resultsIN)
        flight2ari.visibility = View.VISIBLE
        flight2ari.startAnimation(resultsIN)
        line1.visibility = View.VISIBLE
        line1.startAnimation(resultsIN)
        charges1.visibility = View.VISIBLE
        charges1.startAnimation(resultsIN)
        charges1Price.visibility = View.VISIBLE
        charges1Price.startAnimation(resultsIN)
        charges2.visibility = View.VISIBLE
        charges2.startAnimation(resultsIN)
        charges2Price.visibility = View.VISIBLE
        charges2Price.startAnimation(resultsIN)
        grandTotal.visibility = View.VISIBLE
        grandTotal.startAnimation(resultsIN)
        grandTotalPrice.visibility = View.VISIBLE
        grandTotalPrice.startAnimation(resultsIN)
        line2.visibility = View.VISIBLE
        line2.startAnimation(resultsIN)
        frame.visibility = View.VISIBLE
        frame.startAnimation(fadeIN)
        proceedBttn.visibility = View.VISIBLE
        proceedBttn.startAnimation(fadeIN)



        //CLICK LISTENER TO PROCEED TO CREDIT CARD INPUT:
        proceedBttn.setOnClickListener {
            TransitionManager.beginDelayedTransition(pasExpand, AutoTransition())
            proceedBttn.visibility = View.GONE
            frame.visibility = View.GONE
            flight1From.visibility = View.GONE
            arrow1.visibility = View.GONE
            flight1To.visibility = View.GONE
            flight1.visibility = View.GONE
            price1.visibility = View.GONE
            flight2From.visibility = View.GONE
            arrow2.visibility = View.GONE
            flight2To.visibility = View.GONE
            flight2.visibility = View.GONE
            price2.visibility = View.GONE


            payExpand1.visibility = View.VISIBLE
            payExpand2.visibility = View.VISIBLE


            passengerDetailName.text = "${name.text} ${surname.text}"
            passengerDetailPassportNbr.text = passportNbr.text.toString()

            val reservationNbr =  "${flightOneBookingNbr}/${flightTwoBookingNbr.drop(3)}"
            ticketReservationNbr.text = reservationNbr


            //VIEW MODEL RECEIVES RESERVATION NBR:
            bookingViewModel.reservationNbr = reservationNbr


            //ANIMATIONSET:
            passengerDetailArrow1.setOnClickListener {
                val arrowRotatorUP = ObjectAnimator.ofFloat(
                    passengerDetailArrow1,
                    View.ROTATION,
                    180f)
//                arrowRotatorUP.duration = 2000

                val arrowRotatorDOWN = ObjectAnimator.ofFloat(
                    passengerDetailArrow1,
                    View.ROTATION,
                    -90f)
//                arrowRotatorDOWN.duration = 2000

                if (header4.visibility == View.GONE) {
                    TransitionManager.beginDelayedTransition(passengerDetailCard, AutoTransition())
                    arrowRotatorUP.start()
                    header4.visibility = View.VISIBLE
                    passengerDetailPassportNbr.visibility = View.VISIBLE
                    TransitionManager.beginDelayedTransition(payExpand2, AutoTransition())
                    ccBack.visibility = View.GONE
                    ccFront.visibility = View.GONE
                    ccInputDone.visibility = View.GONE

                  } else {
                    TransitionManager.beginDelayedTransition(passengerDetailCard, AutoTransition())
                    passengerDetailPassportNbr.visibility = View.GONE
                    header4.visibility = View.GONE
                    arrowRotatorDOWN.start()

                }
             }


            passengerCCCard.visibility = View.VISIBLE
//            header6.visibility = View.VISIBLE



            //ANIMATIONSET FOR ROTATION OVERVIEW CARDS:
            passengerDetailArrow2.setOnClickListener {
                val arrowRotatorUP2 = ObjectAnimator.ofFloat(
                    passengerDetailArrow2,
                    View.ROTATION,
                    180f)
//                arrowRotatorUP2.duration = 2000

                val arrowRotatorDOWN2 = ObjectAnimator.ofFloat(
                    passengerDetailArrow2,
                    View.ROTATION,
                    -90f)
//                arrowRotatorDOWN2.duration = 2000


                if (ccBack.visibility == View.GONE) {
                    TransitionManager.beginDelayedTransition(passengerCCCard, AutoTransition())
                    arrowRotatorUP2.start()
                    ccFront.visibility = View.VISIBLE
                    ccBack.visibility = View.VISIBLE
                    ccInputDone.visibility = View.VISIBLE
                    header4.visibility = View.GONE
                    passengerDetailPassportNbr.visibility = View.GONE



                    ccInputDone.setOnClickListener {
                        if (checkCCinput(ccBackCardNbr,ccBackCvv, ccBackHolder, ccBackValid)){
                            ccFrontCardNbr.setText(ccBackCardNbr.text)
                            val cardHolderName = ccBackHolder.text
                            ccFrontHolder.setText(cardHolderName)
                            ccFrontValid.setText(ccBackValid.text)


                            //ANIMATION OF CREDIT CARD:
                            var front_anim: AnimatorSet
                            var back_anim: AnimatorSet
                            var bttnFront_anim: AnimatorSet
                            var bttnBack_anim: AnimatorSet

                            var isFront = false
                            val scaleCam: Float = requireContext().resources.displayMetrics.density

                            ccFront.cameraDistance = 8000 * scaleCam
                            ccBack.cameraDistance = 8000 * scaleCam

                            front_anim = AnimatorInflater.loadAnimator(requireContext(), R.animator.card_front_anim) as AnimatorSet
                            back_anim = AnimatorInflater.loadAnimator(requireContext(), R.animator.card_back_anim) as AnimatorSet

                            bttnFront_anim = AnimatorInflater.loadAnimator(requireContext(), R.animator.card_front_anim) as AnimatorSet
                            bttnBack_anim = AnimatorInflater.loadAnimator(requireContext(), R.animator.card_back_anim) as AnimatorSet

                                if(isFront){
                                    front_anim.setTarget(ccFront)
                                    back_anim.setTarget(ccBack)
                                    front_anim.start()
                                    back_anim.start()

                                    bttnFront_anim.setTarget(ccInputContinue)
                                    bttnBack_anim.setTarget(ccInputDone)
                                    bttnBack_anim.start()
                                    ccInputContinue.visibility = View.GONE
                                    bttnFront_anim.start()
                                    ccInputDone.visibility = View.VISIBLE
                                    isFront = false

                                } else {
                                    front_anim.setTarget(ccBack)
                                    back_anim.setTarget(ccFront)
                                    back_anim.start()
                                    front_anim.start()

                                    bttnFront_anim.setTarget(ccInputDone)
                                    bttnBack_anim.setTarget(ccInputContinue)
                                    bttnBack_anim.start()
                                    ccInputDone.visibility = View.GONE
                                    bttnFront_anim.start()
                                    ccInputContinue.visibility = View.VISIBLE
                                    isFront = true
                                }


                            if (ccInputDone.text == "Change Credit Card"){
                                TransitionManager.beginDelayedTransition(pasExpand, AutoTransition())
                                payExpand3.visibility = View.VISIBLE

                            } else {
                                TransitionManager.beginDelayedTransition(pasExpand, AutoTransition())
                                payExpand3.visibility = View.GONE
                            }



                        } else {
                            showErrorSnackBar("Please provide all fields to continue the payment procedure")
                        }
                    }

                } else {
                    TransitionManager.beginDelayedTransition(passengerCCCard, AutoTransition())
                    arrowRotatorDOWN2.start()
                    ccFront.visibility = View.GONE
                    ccBack.visibility = View.GONE
                    ccInputDone.visibility = View.GONE
                    ccInputContinue.visibility = View.GONE
                    TransitionManager.beginDelayedTransition(passengerCCCard, AutoTransition())
                    payExpand3.visibility = View.GONE
                }

                    //CLICK LISTENER TO CONTINUE FINAL PAYMENT:
                    ccInputContinue.setOnClickListener {
                        TransitionManager.beginDelayedTransition(passengerCCCard, AutoTransition())
                        arrowRotatorDOWN2.start()

                        ccFront.visibility = View.GONE
                        ccBack.visibility = View.GONE
                        ccInputDone.visibility = View.GONE
                        ccInputContinue.visibility = View.GONE
                        ccFrontBackground2.visibility = View.VISIBLE
                        payExpand3.visibility = View.VISIBLE



                    //CLICK LISTENER TO CONTINUE FINAL PAYMENT:
                    payNowBttn.setOnClickListener {
                        TransitionManager.beginDelayedTransition(pasExpand, AutoTransition())
                        pasExpand.visibility = View.GONE
                        paymentDoneConstraint.visibility = View.VISIBLE
                        paymentDoneConstraint.startAnimation(resultsIN)

                        //METHOD TO PLAY VIDEO CLIP:
                        setUpRawVideo(paymentDoneVideo)


                        //CLICK LISTENER TO MOVE FROM BOOK FRAGMENT TO TRIPS FRAGMENT:
                        goToBookingBttn.setOnClickListener {
                            val booking_ticketReservationNbr = ticketReservationNbr.text

                            val booking_flight1_flightNbr = flightNbr1.text
                            val booking_flight1_departDate = date1One.text
                            val booking_flight1_departCity = flight1From.text
                            val booking_flight1_departIATA = binding.bigBookCard.cvBookField.tv_IATAdeparture.text.toString()
                            val booking_flight1_ariDate = date1Two.text
                            val booking_flight1_ariCity = flight1To.text
                            val booking_flight1_ariIATA = binding.bigBookCard.cvBookField.tv_IATAarrival.text.toString()
                            val booking_flight1_takeoffTime = time1One.text
                            val booking_flight1_touchdownTime = time1Two.text
                            val booking_flight1_duration = ""

                            val booking_flight1_passFirstname = name.text
                            val booking_flight1_passSurname = surname.text
                            val booking_flight1_cabinclass = cabin1.text
                            val booking_flight1_price = "EUR ${charge1CalcA.toString()}"

                            val booking_flight2_flightNbr = flightNbr2.text
                            val booking_flight2_departDate = date2One.text
                            val booking_flight2_departCity = flight2From.text
                            val booking_flight2_departIATA = binding.bigBookCard.cvBookField.tv_IATAarrival.text.toString()
                            val booking_flight2_ariDate = date2Two.text
                            val booking_flight2_ariCity = flight2To.text
                            val booking_flight2_ariIATA = binding.bigBookCard.cvBookField.tv_IATAdeparture.text.toString()
                            val booking_flight2_takeoffTime = time2One.text
                            val booking_flight2_touchdownTime = time2Two.text
                            val booking_flight2_duration = ""

                            val booking_flight2_passFirstname = name.text
                            val booking_flight2_passSurname = surname.text
                            val booking_flight2_cabinclass = cabin2.text
                            val booking_flight2_price = "EUR ${charge1CalcB.toString()}"



                            //CREATING A NEW BOOKING INSTANCE TO PASS VALUES TO VIEWMODEL:
                            val newBooking = Booking(
                                booking_ticketReservationNbr.toString(),
                                booking_flight1_flightNbr.toString(),
                                booking_flight1_departDate.toString(),
                                booking_flight1_departCity.toString(),
                                booking_flight1_departIATA.toString(),
                                booking_flight1_ariDate.toString(),
                                booking_flight1_ariCity.toString(),
                                booking_flight1_ariIATA.toString(),
                                booking_flight1_takeoffTime.toString(),
                                booking_flight1_touchdownTime.toString(),
                                booking_flight1_duration.toString(),
                                booking_flight1_passFirstname.toString(),
                                booking_flight1_passSurname.toString(),
                                booking_flight1_cabinclass.toString(),
                                booking_flight1_price.toString(),
                                booking_flight2_flightNbr.toString(),
                                booking_flight2_departDate.toString(),
                                booking_flight2_departCity.toString(),
                                booking_flight2_departIATA.toString(),
                                booking_flight2_ariDate.toString(),
                                booking_flight2_ariCity.toString(),
                                booking_flight2_ariIATA.toString(),
                                booking_flight2_takeoffTime.toString(),
                                booking_flight2_touchdownTime.toString(),
                                booking_flight2_duration.toString(),
                                booking_flight2_passFirstname.toString(),
                                booking_flight2_passSurname.toString(),
                                booking_flight2_cabinclass.toString(),
                                booking_flight2_price.toString()
                            )

                            bookingViewModel.insertBooking(newBooking)
                            flightViewModel.paymentCompleted.value = true
                            binding.paymentSummaryCard.videoPreview.visibility = View.VISIBLE



                            //RESETTING BOOKING PROCEDURE:
                            resetBooking()


                        }

                    }

                }

            }

        }

    }


    //RESETTING BOOKING PROCEDURE:
    private fun resetBooking() {
    }



    //HANDLING VIDEO PLAYBACK:
    private fun setUpRawVideo(paymentDoneVideo: VideoView) {
        val mediaController = MediaController(requireContext())
        val videoContent : Uri = Uri.parse("android.resource://" + requireActivity().packageName + "/"+ R.raw.target_transfer)

        paymentDoneVideo.setVideoURI(videoContent)
        paymentDoneVideo.setMediaController(mediaController)
        mediaController.setAnchorView(paymentDoneVideo)
        paymentDoneVideo.requestFocus()
        paymentDoneVideo.start()
    }



    //HANDLING CREDIT CARD ANIMATION:
    private fun ccFlipAnim(ccFront: CardView, ccBack: CardView, ccInputDone: Button) {
        var front_anim: AnimatorSet
        var back_anim: AnimatorSet
        var isFront = true
        val scaleCam: Float = requireContext().resources.displayMetrics.density

        ccFront.cameraDistance = 8000 * scaleCam
        ccBack.cameraDistance = 8000 * scaleCam

        front_anim = AnimatorInflater.loadAnimator(requireContext(), R.animator.card_front_anim) as AnimatorSet
        back_anim = AnimatorInflater.loadAnimator(requireContext(), R.animator.card_back_anim) as AnimatorSet

        ccInputDone.setOnClickListener {
            if(isFront){
                front_anim.setTarget(ccFront)
                back_anim.setTarget(ccBack)
                front_anim.start()
                back_anim.start()
                isFront = false
                ccInputDone.text = "Change Credit Card"
            } else {
                front_anim.setTarget(ccBack)
                back_anim.setTarget(ccFront)
                back_anim.start()
                front_anim.start()
                isFront = true
                ccInputDone.text = "Done"
            }
        }
    }



    //METHOD TO CHECK CREDIT CARD INFO:
    private fun checkCCinput(
        ccBackCardNbr: EditText,
        ccBackCvv: EditText,
        ccBackHolder: EditText,
        ccBackValid: EditText,
    ): Boolean {

        return when {
            TextUtils.isEmpty(ccBackCardNbr.text.toString()) -> {
                showErrorSnackBar("Please enter a credit card number")
                false
            }
            TextUtils.isEmpty(ccBackCvv.text.toString()) -> {
                showErrorSnackBar("Please enter the credit card's CVV code")
                false
            }
            TextUtils.isEmpty(ccBackHolder.text.toString()) -> {
                showErrorSnackBar("Please enter the credit card's holder name")
                false
            }
            TextUtils.isEmpty(ccBackValid.text.toString()) -> {
                showErrorSnackBar("Please enter the credit card's expiration date")
                false
            } else -> {
                true
            }
        }
    }



}










/*    Transportr
 *    Copyright (C) 2013 - 2017 Torsten Grote
 *
 *    This program is Free Software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as
 *    published by the Free Software Foundation, either version 3 of the
 *    License, or (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.grobox.liberario.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Date;
import java.util.SortedSet;
import java.util.TreeSet;

import de.grobox.liberario.R;
import de.grobox.liberario.WrapLocation;
import de.grobox.liberario.activities.NewMapActivity;
import de.grobox.liberario.departures.DeparturesActivity;
import de.grobox.liberario.departures.DeparturesLoader;
import de.grobox.liberario.utils.TransportrUtils;
import de.schildbach.pte.dto.Departure;
import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.LineDestination;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.StationDepartures;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.google.common.base.Strings.isNullOrEmpty;
import static de.grobox.liberario.departures.DeparturesActivity.MAX_DEPARTURES;
import static de.grobox.liberario.departures.DeparturesLoader.getBundle;
import static de.grobox.liberario.utils.Constants.LOADER_DEPARTURES;
import static de.grobox.liberario.utils.Constants.WRAP_LOCATION;
import static de.grobox.liberario.utils.TransportrUtils.getCoordinationName;
import static de.grobox.liberario.utils.TransportrUtils.getDrawableForLocation;
import static de.grobox.liberario.utils.TransportrUtils.startGeoIntent;
import static de.schildbach.pte.dto.QueryDeparturesResult.Status.OK;

public class LocationFragment extends BottomSheetDialogFragment
		implements LoaderManager.LoaderCallbacks<QueryDeparturesResult> {

	public static final String TAG = LocationFragment.class.getName();

	private NewMapActivity activity;
	private WrapLocation location;
	private SortedSet<Line> lines = new TreeSet<>();
	private ViewGroup linesLayout;
	private Button nearbyStationsButton;
	private ProgressBar nearbyStationsProgress;

	public static LocationFragment newInstance(WrapLocation location) {
		LocationFragment f = new LocationFragment();

		Bundle args = new Bundle();
		args.putSerializable(WRAP_LOCATION, location);
		f.setArguments(args);

		return f;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		activity = (NewMapActivity) getActivity();

		Bundle args = getArguments();
		location = (WrapLocation) args.getSerializable(WRAP_LOCATION);
		if (location == null) throw new IllegalArgumentException("No location");

		View v = inflater.inflate(R.layout.fragment_location, container, false);

		// Directions
		FloatingActionButton directionFab = (FloatingActionButton) v.findViewById(R.id.directionFab);
		directionFab.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				Log.e("TEST", "directionFab clicked");
			}
		});

		// Location
		final ImageView locationIcon = (ImageView) v.findViewById(R.id.locationIcon);
		locationIcon.setImageDrawable(getDrawableForLocation(getContext(), location, false));
		TextView locationName = (TextView) v.findViewById(R.id.locationName);
		if (location != null) {
			locationName.setText(location.getName());
			if (location.getLocation().products != null) {
				Log.e("TEST", location.getLocation().products.toString());
			}
		}
		OnClickListener locationClick = new OnClickListener() {
			@Override
			public void onClick(View view) {
				activity.zoomTo(location);
			}
		};
		locationIcon.setOnClickListener(locationClick);
		locationName.setOnClickListener(locationClick);

		// Lines
		linesLayout = (ViewGroup) v.findViewById(R.id.linesLayout);
		linesLayout.setVisibility(GONE);

		// Location Info
		TextView locationInfo = (TextView) v.findViewById(R.id.locationInfo);
		StringBuilder locationInfoStr = new StringBuilder();
		if (!isNullOrEmpty(location.getLocation().place)) {
			locationInfoStr.append(location.getLocation().place);
		}
		if (location.getLocation().hasLocation()) {
			if (locationInfoStr.length() > 0) locationInfoStr.append(", ");
			locationInfoStr.append(getCoordinationName(location.getLocation()));
		}
		locationInfo.setText(locationInfoStr);

		// Departures
		Button departuresButton = (Button) v.findViewById(R.id.departuresButton);
		if (location.hasId()) {
			departuresButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					Intent intent = new Intent(getContext(), DeparturesActivity.class);
					intent.putExtra(WRAP_LOCATION, location);
					startActivity(intent);
				}
			});
		} else {
			departuresButton.setVisibility(GONE);
		}

		// Nearby Stations
		nearbyStationsButton = (Button) v.findViewById(R.id.nearbyStationsButton);
		nearbyStationsProgress = (ProgressBar) v.findViewById(R.id.nearbyStationsProgress);
		nearbyStationsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				Log.e("TEST", "nearbyStationsButton clicked");
				nearbyStationsButton.setVisibility(INVISIBLE);
				nearbyStationsProgress.setVisibility(VISIBLE);
				activity.findNearbyStations(location.getLocation());
			}
		});

		// Share Location
		Button shareButton = (Button) v.findViewById(R.id.shareButton);
		shareButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				startGeoIntent(getContext(), location.getLocation());
			}
		});

		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (location.hasId()) {
			Bundle args = getBundle(location.getId(), new Date(), MAX_DEPARTURES);
			getLoaderManager().initLoader(LOADER_DEPARTURES, args, this).forceLoad();
		}
	}

	@Override
	public DeparturesLoader onCreateLoader(int id, Bundle args) {
		return new DeparturesLoader(getContext(), args);
	}

	@Override
	public void onLoadFinished(Loader<QueryDeparturesResult> loader, QueryDeparturesResult data) {
		if (data.status == OK) {
			for (StationDepartures s : data.stationDepartures) {
				if (s.lines != null) {
					for (LineDestination d : s.lines) lines.add(d.line);
				}
				for (Departure d : s.departures) lines.add(d.line);
			}
			linesLayout.removeAllViews();
			for (Line l : lines) {
				TransportrUtils.addLineBox(getContext(), linesLayout, l);
			}
			linesLayout.setAlpha(0f);
			linesLayout.setVisibility(VISIBLE);
			linesLayout.animate().alpha(1f).start();
		}
	}

	@Override
	public void onLoaderReset(Loader<QueryDeparturesResult> loader) {
	}

	public void onNearbyStationsLoaded() {
		nearbyStationsButton.setVisibility(VISIBLE);
		nearbyStationsButton.setEnabled(false);
		nearbyStationsProgress.setVisibility(INVISIBLE);
	}

}
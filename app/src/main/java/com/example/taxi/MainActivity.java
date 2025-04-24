package com.example.taxi;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.InputListener;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.runtime.image.ImageProvider;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements InputListener {
    private MapView mapView;
    private FusedLocationProviderClient fusedLocationClient;
    private PlacemarkMapObject firstPoint;
    private PlacemarkMapObject secondPoint;
    private ImageButton btnGenerate;
    private Point basePoint;
    private final Random random = new Random();
    private static final double FIXED_DISTANCE_KM = 0.5;
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final int LOCATION_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapKitFactory.setApiKey("3958832b-3a59-442c-a2a5-000c9b388163");
        MapKitFactory.initialize(this);
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapView);
        btnGenerate = findViewById(R.id.btnGenerate);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupMap();
        requestLocationPermission();
        setupButton();
    }

    private void setupMap() {
        mapView.getMap().addInputListener(this);
    }

    private void requestLocationPermission() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_CODE);
        } else {
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    basePoint = new Point(location.getLatitude(), location.getLongitude());
                    updateFirstPoint(basePoint);
                } else {
                    Toast.makeText(this, "Не удалось получить местоположение", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateFirstPoint(Point point) {
        if (firstPoint == null) {
            firstPoint = mapView.getMap().getMapObjects().addPlacemark(point);
            firstPoint.setIcon(ImageProvider.fromResource(this, R.drawable.placeholder));
        } else {
            firstPoint.setGeometry(point);
        }
        mapView.getMap().move(new CameraPosition(point, 15f, 0f, 0f));
    }

    private void setupButton() {
        btnGenerate.setOnClickListener(v -> {
            if (basePoint != null) {
                generateSecondPoint();
            } else {
                Toast.makeText(this, "Сначала установите базовую точку", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void generateSecondPoint() {
        double azimuth = random.nextDouble() * 360;
        Point newPoint = calculatePoint(basePoint, azimuth);

        if (secondPoint == null) {
            secondPoint = mapView.getMap().getMapObjects().addPlacemark(newPoint);
            secondPoint.setIcon(ImageProvider.fromResource(this, R.drawable.placeholder));
        } else {
            secondPoint.setGeometry(newPoint);
        }
    }

    private Point calculatePoint(Point origin, double azimuthDegrees) {
        double lat1 = Math.toRadians(origin.getLatitude());
        double lon1 = Math.toRadians(origin.getLongitude());
        double azimuth = Math.toRadians(azimuthDegrees);
        double angularDistance = MainActivity.FIXED_DISTANCE_KM / EARTH_RADIUS_KM;

        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(angularDistance) +
                Math.cos(lat1) * Math.sin(angularDistance) * Math.cos(azimuth));
        double lon2 = lon1 + Math.atan2(Math.sin(azimuth) * Math.sin(angularDistance) * Math.cos(lat1),
                Math.cos(angularDistance) - Math.sin(lat1) * Math.sin(lat2));

        return new Point(Math.toDegrees(lat2), Math.toDegrees(lon2));
    }

    @Override
    public void onMapTap(@NonNull Map map, @NonNull Point point) {
        basePoint = point;
        updateFirstPoint(point);
        if (secondPoint != null) {
            generateSecondPoint();
        }
    }

    @Override
    public void onMapLongTap(@NonNull Map map, @NonNull Point point) {}

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        MapKitFactory.getInstance().onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        mapView.onStop();
        MapKitFactory.getInstance().onStop();
        super.onStop();
    }
}
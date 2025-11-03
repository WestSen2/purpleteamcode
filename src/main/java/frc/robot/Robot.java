package frc.robot;

import com.ctre.phoenix6.controls.DutyCycleOut;
import com.ctre.phoenix6.hardware.TalonFX;
import com.revrobotics.ColorSensorV3;

import edu.wpi.first.wpilibj.I2C;
import edu.wpi.first.wpilibj.PS4Controller;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.Timer;

public class Robot extends TimedRobot {

    // --- Color Sensor ---
    private final I2C.Port i2cPort = I2C.Port.kOnboard;
    private ColorSensorV3 colorSensor;
    private double lastPrintTime = 0;
    private static final double PRINT_INTERVAL = 0.5;

    // --- Motor Control ---
    private TalonFX motor;
    private DutyCycleOut dutyCycleControl;

    // --- PS4 Controller ---
    private PS4Controller controller;

    @Override
    public void robotInit() {
        if (RobotBase.isReal()) {
            System.out.println("*** RUNNING ON REAL ROBOT ***");
        } else {
            System.out.println("*** RUNNING IN SIMULATION ***");
        }

        // Initialize motor on CANivore bus
        try {
            motor = new TalonFX(1, "canivore");
            dutyCycleControl = new DutyCycleOut(0.0);
            System.out.println("✓ Motor initialized successfully on CANivore");
        } catch (Exception e) {
            System.out.println("✗ WARNING: Motor not connected - motor control disabled");
            System.out.println("  Error: " + e.getMessage());
            motor = null;
        }

        // Initialize PS4 controller
        try {
            controller = new PS4Controller(0);
            System.out.println("✓ PS4 controller initialized on USB port 0");
        } catch (Exception e) {
            System.out.println("✗ WARNING: PS4 controller initialization failed");
            System.out.println("  Error: " + e.getMessage());
            controller = null;
        }

        // Initialize color sensor
        try {
            colorSensor = new ColorSensorV3(i2cPort);
            System.out.println("✓ Color sensor object created");

            if (!colorSensor.isConnected()) {
                System.out.println("✗ WARNING: Sensor reports NOT CONNECTED!");
            } else {
                System.out.println("✓ Sensor reports CONNECTED");
            }
        } catch (Exception e) {
            System.out.println("✗ WARNING: Color sensor initialization failed!");
            e.printStackTrace();
            colorSensor = null;
        }

        System.out.println("\n=== INITIALIZATION COMPLETE ===\n");
    }

    @Override
    public void teleopPeriodic() {
        if (motor == null || controller == null) {
            System.out.println("✗ Motor or controller not initialized. Skipping teleopPeriodic.");
            return;
        }

        double speed = 0.0;

        if (controller.getCrossButton()) {
            speed = 0.7;
            System.out.println("Cross pressed - Speed: 0.7");
        } else if (controller.getCircleButton()) {
            speed = 0.8;
            System.out.println("Circle pressed - Speed: 0.8");
        } else if (controller.getSquareButton()) {
            speed = 0.75;
            System.out.println("Square pressed - Speed: 0.75");
        } else if (controller.getTriangleButton()) {
            speed = 0.0;
            System.out.println("Triangle pressed - STOP");
        }

        // Set motor speed
        dutyCycleControl = new DutyCycleOut(speed);
        motor.set(speed);
    }

    @Override
public void robotPeriodic() {
    double currentTime = Timer.getFPGATimestamp();

    if (colorSensor == null) {
        if (currentTime - lastPrintTime >= 2.0) {
            lastPrintTime = currentTime;
            System.out.println("⚠ Color sensor is NULL - not initialized properly");
        }
        return;
    }

    if (currentTime - lastPrintTime >= PRINT_INTERVAL) {
        lastPrintTime = currentTime;

        try {
            var color = colorSensor.getColor();
            int proximity = colorSensor.getProximity();

            String detectedColor = "Unknown";
            double r = color.red;
            double g = color.green;
            double b = color.blue;

            double max = Math.max(r, Math.max(g, b));
            double min = Math.min(r, Math.min(g, b));
            double delta = max - min;

            // --- Compute hue in degrees (0–360) ---
            double hue = 0.0;
            if (delta == 0) {
                hue = 0;
            } else if (max == r) {
                hue = 60 * (((g - b) / delta) % 6);
            } else if (max == g) {
                hue = 60 * (((b - r) / delta) + 2);
            } else {
                hue = 60 * (((r - g) / delta) + 4);
            }
            if (hue < 0) hue += 360;

            // --- Compute brightness and saturation (optional for white detection) ---
            double saturation = (max == 0) ? 0 : (delta / max);

            // --- Classify color ---
            if (max < 0.2) {
                detectedColor = "Black/Dark";
            } else if (saturation < 0.2 && max > 0.4) {
                detectedColor = "White";
            } else if (hue >= 330 || hue < 30) {
                detectedColor = "Red";
            } else if (hue >= 30 && hue < 90) {
                detectedColor = "Yellow";
            } else if (hue >= 90 && hue < 150) {
                detectedColor = "Green";
            } else if (hue >= 150 && hue < 210) {
                detectedColor = "Cyan";
            } else if (hue >= 210 && hue < 270) {
                detectedColor = "Blue";
            } else if (hue >= 270 && hue < 330) {
                detectedColor = "Purple";
            }

            System.out.printf("Detected: %-7s | R: %.2f G: %.2f B: %.2f | Hue: %.1f | Proximity: %d%n",
                    detectedColor, r, g, b, hue, proximity);

        } catch (Exception e) {
            System.out.println("✗ ERROR reading color sensor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
}
# **Taxiflay**

## **Project Description**
Taxiflay is an Android application built with Kotlin that simulates a taxi meter. It calculates the fare in real-time based on distance traveled, trip duration, and other parameters, such as base fare, fare per kilometer, and fare per minute. The app includes features like real-time geolocation tracking, location access permission management, and displaying driver information.

---

## **Objectives & Features**

### **Real-time Driver Location**
- Displays the driver's current position on a **Google Maps** map, updating in real-time.

### **Dynamic Fare Calculation**
- The fare is dynamically calculated based on:
  - **Base Fare**: A fixed price at the start of the trip.
  - **Fare per Kilometer**: A price based on distance traveled (e.g., 1.5 DH/km).
  - **Fare per Minute**: A price based on time spent (e.g., 0.5 DH/min).

### **Notifications**
- Sends a notification at the end of the trip with:
  - Distance traveled
  - Time elapsed
  - Total fare

### **Permissions Management**
- Requests **location access** at runtime using the **EasyPermissions** library for simplified permission handling.

### **User Interface**
- The main interface includes:
  - A **TextView** displaying the distance traveled.
  - A **TextView** displaying the time elapsed.
  - A **TextView** displaying the total fare.
  - A **Button** to start the trip.
  - A **Google Maps** map to show the driver's position in real-time.

### **Distance & Time Calculation**
- Uses **FusedLocationProviderClient** to calculate distance and time in real-time.

---

## **Pages**

### **Login Page**
The login page allows users to sign in to their accounts easily.

#### Overview
![Login Page](https://github.com/user-attachments/assets/defb1589-a58b-4496-a5e8-9503dab8596b)

### **Signup Page**
The signup page provides users with a smooth and user-friendly registration experience.

#### Overview
![Signup Page](https://github.com/user-attachments/assets/b401bfbd-08c1-4c7a-9f78-01d3ab4ea830)

### **Home**
The home page shows the taxi app interface. The taxi status is 'Available,' with a distance of 0 km, a duration of 0 minutes, and a fare of 0 DH. The 'Stop Trip' button is visible to end the trip. A base fare of 2.5 DH is displayed at the bottom.

#### Overview
![Home Page](https://github.com/user-attachments/assets/7c9f40c3-28fb-444c-a404-143f401db010)

### **Profile**
The profile page allows users to update their personal information, including name, email, age, and license type. Changes can be saved with the 'Save' button, and a QR code is displayed at the bottom.

#### Overview
![Profile Page](https://github.com/user-attachments/assets/a649e603-4850-44a9-b9c0-9eb050c8890b)

### **History**
The history page displays a list of previous trips, including details like the trip date, distance, duration, and fare.

#### Overview
![History Page](https://github.com/user-attachments/assets/f07d416a-6980-4beb-8302-e6121d8a0d2d)

---

## **Technologies Used**
- **Kotlin**: The programming language used for Android development.
- **Google Maps API**: To display real-time driver location on the map.
- **FusedLocationProviderClient**: For accurate location tracking and distance calculation.
- **EasyPermissions**: To handle location permissions in a simplified way.

---

## **Installation**
https://play.google.com/store/apps/details?id=com.myapp.taximeter&pcampaignid=web_share

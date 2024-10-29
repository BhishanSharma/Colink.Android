# CoLink App
## Overview

CoLink is an innovative application designed for fast file sharing within a local network, offering speeds up to 8 MB/s. The app facilitates seamless file transfers by leveraging device hotspots, ensuring efficient and high-speed data exchange without the need for mobile data or an internet connection.

## Features

- **High-Speed File Sharing**: Transfer files at speeds up to 8 MB/s.
- **Local Network Connectivity**: Utilizes local network via device hotspots for quick and secure file sharing.
- **Intuitive User Interface**: Easy-to-use interface for smooth navigation and file transfer.
- **Hotspot-Based Transfer**: Receiver turns on the hotspot, and the sender connects to it, ensuring both devices' mobile data are turned off.
- **Error Handling**: Robust mechanisms to manage and resolve any issues during file transfers.

## Installation

1. **Clone the Repository**: 
    ```sh
    git clone https://github.com/yourusername/colink-app.git
    ```
2. **Open in Android Studio**:
    - Open Android Studio.
    - Select "Open an existing project".
    - Navigate to the cloned repository and open it.
3. **Build the Project**:
    - Click on "Build" in the top menu.
    - Select "Make Project" to build the project.
4. **Run the App**:
    - Connect your Android device or start an AVD.
    - Click on the "Run" button in Android Studio or use `Shift + F10`.

## Usage

1. **Launching the App**:
    - Upon launching, you will be greeted with the main interface.
    - Navigate through the app to start sharing files.

2. **File Sharing**:
    - **Receiver**: Turn on the hotspot from your device settings.
    - **Sender**: Connect to the receiver’s hotspot.
    - Ensure both devices have their mobile data turned off.
    - Select the files you want to share and initiate the transfer.

3. **Service Interaction**:
    - The app manages the connection and file transfer processes.
    - Notifications will update based on the transfer status.

## Code Structure

- **MainActivity**: Hosts the primary UI and initiates the file transfer process.
- **FileTransferFragment**: Contains the UI elements and controls for selecting and sharing files.
- **HotspotManager**: Manages hotspot creation and connection for seamless file transfers.
- **TransferService**: Handles the file transfer, ensuring high speed and reliability.
- **NotificationUtils**: Utility class for creating and managing transfer notifications.
- **Error Handling**: Integrated within functions to manage exceptions and ensure smooth operation.

## Contributing

1. **Fork the Repository**:
    - Click on the "Fork" button on the repository page to create your own fork.
2. **Create a Branch**:
    ```sh
    git checkout -b feature/your-feature-name
    ```
3. **Commit Your Changes**:
    ```sh
    git commit -m "Add some feature"
    ```
4. **Push to the Branch**:
    ```sh
    git push origin feature/your-feature-name
    ```
5. **Create a Pull Request**:
    - Go to the repository on GitHub and create a pull request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contact

For any questions or feedback, please contact:

- **Name**: Bhishan sharma
- **Email**: bhishansharma3354@gmail.com
- **GitHub**: [Bhishan-sharma](https://github.com/Bhishan-sharma)

---

Enjoy using CoLink for fast and efficient file sharing within your local network!

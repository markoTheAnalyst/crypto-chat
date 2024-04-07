# Crypto Chat Application

This is a secure chat application that enables protected communication between users without utilizing built-in classes providing such functionality. The application facilitates message exchange between two users by leaving files at specified locations on the file system.

Upon logging into the system, the application displays information about currently active users, allowing the logged-in user to select a user to initiate a session with. Both users must consent to participate in the session before commencing communication. All communication between session participants is protected via appropriate cryptographic techniques and algorithms.

The first and last messages within a session are additionally concealed using steganography. When sending these messages the path to the image in which the message will be hidden after encryption must be provided.

Chat functionality is implemented using the file system, which, in this case, simulates a distributed client-server platform for communication between remote users. By default, each user is assigned an appropriate directory representing their inbox. The application should monitor and detect changes in the user's directory, i.e., detect the arrival of new messages. After the user reads a message within the application, it is deleted from the file system.

## Features

- Message exchange via leaving files in designated locations.
- Session initiation, maintenance, and termination at the application layer.
- Steganographic concealment of first and last messages.
- Simulation of a distributed client-server platform using the file system.
- Real-time monitoring and detection of new messages.

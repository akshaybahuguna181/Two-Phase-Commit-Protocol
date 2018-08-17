# Two-Phase-Commit-Protocol
Distributed Systems Lab Assignment 2. Implemented the 2 Phase Commit Protocol in Java using SWING

Two Phase Commit
Two Phase Commit Protocol is used to ensure atomic transactions among distributed systems. That is, it is to ensure that a distributed transaction occurs at either all places or at no places at all. A coordinator manages this transaction among various participants. Initially coordinator is in INIT phase and it sends a vote request (a string) to participants. Each participant can either vote to commit or abort. If all vote commit then coordinator initiates a global commit stating each participant can store that data in non-volatile memory (file). If even one participant votes to abort, it initiates a global abort. If one or more participants are not able to vote in a stipulated time period, it again initiates global abort.

The following features are covered:
1.	Client and server processes work successfully.
2.	Participant, Coordinator and Server have a GUI.
3.	Participants correctly transition states according to 2PC
4.	Coordinator correctly transitions states according to 2P
5.	Participants handle timeouts according to 2PC
6.	Coordinator handles timeouts according to 2PC
7.	Server shows HTTP message format
8.	HTTP message formats are valid
9.	Participants save arbitrary string to non-volatile storage
10.	Participants load arbitrary string from non-volatile storage on startup.
11.	Comments are mentioned in code.


Software Requirements:

1.	Eclipse IDE.
2.	JDK 8 or above (build 1.8.0_144-b01).

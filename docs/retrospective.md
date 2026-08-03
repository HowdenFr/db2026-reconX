# Retrospective Document

=========================

## Team US Cohort 1-Team 1

## What Worked:

- Integrating AI such as Claude, Github CoPilot, or CODEX to understand instructions, provide guidance on code installation, and technical coding was helpful.
- Pull Style Workflow. Our team naturally adopted a pull style workflow, where after someone submitted a ticket they would ask for another ticket to work on. While there are assigned "roles" below, everyone worked on all parts of the project.
- Testing every step of the way. At the end of every ticket there were terminal code used to test every ticket. Our team prioritized these tests and wouldn't move onto other dependent tickets until it passed the tests.

## What Didn't Work Well:

- We were unable to read the instructions without the help of AI. Part of this was because of the vagueness of instructions and the other part was the newness of certain code like SpringBoot, Kafka, Grafana, Mvnw, Promethieus. Most of our team hadn't worked with this advance code and if they had, it was at a novice level. As such our team relied on AI, the instructors help, and Hint 4 on some tickets.
- Understanding the full picture. Each day our team would work on tickets and submit them once we were done. We would focus on the micro perspective of the project and not the macro side. If we saw the larger picture of the project, we could have written code that could have helped future tickets down the road.
- Understanding what versions of code we needed. The main culprit was understanding that we needed Java 25 and not the local virtual machine copy of Java 17. This delayed our testing which is explained below.
- Debugging. This project was so large and complex for us that if one piece of code didn't work, we had no idea how to fix it. Thus we would ask AI to understand the issue and while most of the time it would work, we would know how AI fixed the problem.

## What Would You Change?

- The instructions. Every ticket we were scratching our heads, trying to understand what the ticket was asking. If the instructions had been more clear, the project would have gone smoother and our reliance on AI would have been less.
- The scope of the project. Making a trade website with a secure backend and updated front end is an effective project for graduates in training to get into. However, introducing ideas like Grafana, .YML, .XML that aren't taught in the training makes the project overloading. Scaling back the project and only practicing ideas that were taught in the training like connecting a JAVA source to a SQL database or the basics of Springboot would be helpful. Additionally, connecting the project construction similar to project constructions at work would be helpful.
- Creating diagrams for how everything is connected would be helpful. Understanding the big picture would be super helpful.
- Last, make sure to have the code run on Java 17 and not Java 25. Our team spent 2 hours on trying to test our project with springboot only to figure out that we needed to install Java 25 to our VMs.

## What Surprised You?

- How many tests you must make for your code. We never realized how many small tests it would take for our code to run properly.
- How many coding languages and libraries one needs to complete a project. The amount of imports and languages we used in our project was mindboggling.
- Why using Kafka was better than

## Technical Notes For The Next Cohort:

- make sure you install Java 25, Kafka, Docker, update your mvnw, update yourself on GitHub terminal commands, use AI to schedule your team ticket plan each day.

## Team

- Lead : Alora Orr
- Backend: Jerrod Meltzer
- Frontend: Juni Qureshi, Frank Howden
- DevOps/CI: Alora Orr, Jua Augustin

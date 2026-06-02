# AI Agents

in this project there are several HTML files inside the HTML guiding folder

- Those files are for building agents orchestrators for AI developer assistance.
- you can examine the HTML carefully before starting any of the implementations to come up with the appropriate plan
  based on the latest HTML file 05.
- provide me your examination and your implementation plan as a new HTML file 06-Claude-plan

follow these steps when generating the HTML files:

1. Create the basic HTML skeleton.
2. Apply the necessary styling.
3. Add the findings incrementally, one at a time, rather than all at once.
4. After each addition, review the HTML to ensure all findings are included and the context is preserved.
5. Continue this process until all findings are incorporated and verified.

By following this step-by-step approach, you should be able to finalize the HTML page more efficiently and accurately.

## Follow-up Architecture diagram

- Now would like to add under the setup folder another Markdown file that will have an architecture diagram
    - Structural diagram to explain the whole project structure.
    - Workflow of the agents from getting the request till the response
    - any additional diagrams that are required

## Follow-up Design update.

  - From what I can see the design is poor; what I understand is that I need each agent to have some level of access to something external
    - Ticket Analysis → access to Jira somehow
    - Project Setup → the ability to create a folder locally where to add the HTML reports
    - Deep Dive → access to the project code so that we can be able to compare the code with ticket analyses
    - Visual Report → the ability to create an HTML report in the folder where the Project setup agent is done
    - Review → Waiting for the user to approve the steps of implementation from the HTML report or making some iteration to enhance the HTML report
    - Implementation → Starting the code implementation based on the previous approval from the developer
    - QA → check with the developer to review the implementation before going forward
    - Deployment → commit and push the changes to GitHub create a PR, also this agent can handle PR comments by analyzing the PR comments and provide the developer with the ability to update the HTML report with an additional section such as pr comments to be ahndlled.
  - The above agents design is the updated version and the overview of what each agent should do and work, now based on that
    - review the project design and implementation to make sure that the project implementation is aligned with the above design.
    - Check the md files for the agents to make sure that the md files for agents are also aligning with the requirements
  - this is not the end of the design we will be working later on each agent to make sure that the agents are aligned with the requirements.
  - so try to brake the requirments in this request and not working on them at once, to avoid stucking in the middle of the implementation.
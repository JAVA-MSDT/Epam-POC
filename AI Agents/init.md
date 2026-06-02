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
  - so try to break the requirements in this request and not working on them at once, to avoid stucking in the middle of the implementation.

## Follow-up Design update part 02.

- Making deep study of the design, I can see a possible agents merge to be only one to reduce the number of agents
  - Project setup is used only once and usually at the startup of the workflow
  - The Ticket Analysis also is used only once and usually at the startup of the workflow
  - The Deep Dive agent is used only once and usually at the startup of the workflow by taking the Ticket Analysis output as an input to verify it across the codebase.
    - We can merge the above three agents, what do you think about that?
  - The Visual Report will get its input as the output of the above Deep Dive agent.
    - This agent, I think it can be used also later in the subsequent HTML generation, or updating an existing HTML report
  - The Review agent will get some interactions from the user after the user reviews the HTML report.
    - That means there is a chance that the user will ask the agent to do another cycle of the code Deep Dive, and again updating the HTML report.
    - That means that the Deep Dive and the Visual Report agents need to be flexable and reusable, am i right?
  - Implementation agent will get the input from the Review agent and will start the code implementation.
    - This code implementation will be based on the implementation provided by the HTML report as part of it is structure.
    - So that each implementation will be an independent implementation step that can be committed and verified by the user.
  - The QA agent i think will be also working with the implementation agent, so that the user will approve each implementation step or update it.
    - So, I think that the QA and the Implementation agents can be merged to be one agent.
  - The Deployment agent will get the input from the QA agent and will commit the changes one commit at a time after the user approved it to local git.
    - with the final agreed commit from the user, the agenet will push the changes to GitHub and create a PR.
    - Later the agent will listen to the GitHub PR in case of some comments from the user or other team members.
    - The agent will also be able to update the HTML report with the comments from the user.

- The above-updated deigning needs some evaluation and testing, can you please go through it and let me know if it is the right approach, or we need to enhance it or updating, also provide me with anything you can see needed.
- I am providing you with 07-prompts.html, I used to use it for the prompts, I am using them in the manual approach before thinking of creating agents to automate them.
  - can you analyze it and check where each prompt fets in our agents’ design?
- You can provide me with your feedback on the above design as appending section in the 06-Claude-plan.html
  

From what I understand, the llm can have all the code base at the first codebase scan, so that next time it can be less time-consuming and tokens to re-read it again, am I right ?
if yes, that will make the Steps 6 + 7 really reusable and contxt saving.
- also I think that some of those agents will need somehow to track the progress of the implementation, analyzes until we finish the implementation.
- right ? if yes which will have this capability?
package org.usfirst.frc.team5002.robot.commands;

import edu.wpi.first.wpilibj.command.CommandGroup;
import edu.wpi.first.wpilibj.DigitalInput;

/**
 * Yet another autonomous logic command. Probably the right one, though.
 */
public class AutonomousPuguinReal extends CommandGroup {
	private DigitalInput seesaw, gate, kindaLeft, notLeft, moreNotLeft, reallyNotLeft, safetyKillSwitchThing;

	public AutonomousPuguinReal() {
		seesaw = new DigitalInput(0);
		gate = new DigitalInput(1);
		kindaLeft = new DigitalInput(2);
		notLeft = new DigitalInput(3);
		moreNotLeft = new DigitalInput(4);
		reallyNotLeft = new DigitalInput(5);
		safetyKillSwitchThing = new DigitalInput(6);

		// add commands here:

		if (safetyKillSwitchThing.get()) {
			return;
			// if switch is activated, all other programs will be discontinued
		}

		addSequential(new Automove(0, 5));
		// move forward an additional 5 ticks

		if (seesaw.get()) {
			addSequential(new WhatAreThose());
			// lower robot arms
			addSequential(new Automove(0, 10));
			// move past the defense
		}

		else if (gate.get()) {
			addSequential(new Automove(0, 10));
			// move past the defense
		} else {
			addSequential(new Automove(0, 10));
			// move past the defense
		}

		/*
		 * end of defense rotation switch
		 */
		/** 
		 * TODO please clarify on what the heck 'really not left'
		 *  (and others) means.
		 */

		if (kindaLeft.get()) {
			addSequential(new Automove(0, 0));
			// turn the robot to line up with the goal
		}

		else if (notLeft.get()) {
			// not left defense

		}

		else if (moreNotLeft.get()) {
			// more not left defense

		}

		else if (reallyNotLeft.get()) {
			// really not left defense

		}

//		addSequential(new Poi());
		// find the goal
		addSequential(new DoLaunch());
		// Add Commands here:
		// e.g. addSequential(new Command1());
		// addSequential(new Command2());
		// these will run in order.
		// To run multiple commands at the same time,
		// use addParallel()
		// e.g. addParallel(new Command1());
		// addSequential(new Command2());
		// Command1 and Command2 will run in parallel.
		// A command group will require all of the subsystems that each member
		// would require.
		// e.g. if Command1 requires chassis, and Command2 requires arm,
		// a CommandGroup containing them would require both the chassis and the
		// arm.
	}
}

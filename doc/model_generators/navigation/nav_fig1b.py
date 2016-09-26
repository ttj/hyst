'''
Created by Hyst v1.3
Hybrid Automaton in PySim
Converted from file: 
Command Line arguments: -gen nav "-matrix -1.2 0.1 0.1 -1.2 -i_list B 2 4 4 3 4 2 2 A -width 3 -startx 0.5 -starty 1.5 -noise 0.1" -o nav_fig1b.py -tool pysim "-corners True -legend False -rand 100 -time 5 -title nav_fig1b"
'''

import hybridpy.pysim.simulate as sim
from hybridpy.pysim.simulate import init_list_to_q_list, PySimSettings
from hybridpy.pysim.hybrid_automaton import HybridAutomaton, HyperRectangle

def define_ha():
    '''make the hybrid automaton and return it'''

    ha = HybridAutomaton()
    ha.variables = ["x", "y", "xvel", "yvel"]


    mode_0_0 = ha.new_mode('mode_0_0')
    mode_0_0.inv = lambda state: state[0] <= 1 and state[1] <= 1
    mode_0_0.der = lambda _, state: [state[2], state[3], -1.2 * (state[2] - 1) + 0.1 * (state[3] - 0), 0.1 * (state[2] - 1) + -1.2 * (state[3] - 0)]
    mode_0_0.der_interval_list = [[0, 0], [0, 0], [-0.1, 0.1], [-0.1, 0.1]]

    mode_1_0 = ha.new_mode('mode_1_0')
    mode_1_0.inv = lambda state: state[0] >= 1 and state[0] <= 2 and state[1] <= 1
    mode_1_0.der = lambda _, state: [state[2], state[3], -1.2 * (state[2] - 1) + 0.1 * (state[3] - 0), 0.1 * (state[2] - 1) + -1.2 * (state[3] - 0)]
    mode_1_0.der_interval_list = [[0, 0], [0, 0], [-0.1, 0.1], [-0.1, 0.1]]

    mode_2_0 = ha.new_mode('mode_2_0')
    mode_2_0.inv = lambda state: state[0] >= 2 and state[1] <= 1
    mode_2_0.der = lambda _, state: [0, 0, 0, 0]
    mode_2_0.der_interval_list = [[0, 0], [0, 0], [0, 0], [0, 0]]

    mode_0_1 = ha.new_mode('mode_0_1')
    mode_0_1.inv = lambda state: state[0] <= 1 and state[1] >= 1 and state[1] <= 2
    mode_0_1.der = lambda _, state: [state[2], state[3], -1.2 * (state[2] - 0) + 0.1 * (state[3] - -1), 0.1 * (state[2] - 0) + -1.2 * (state[3] - -1)]
    mode_0_1.der_interval_list = [[0, 0], [0, 0], [-0.1, 0.1], [-0.1, 0.1]]

    mode_1_1 = ha.new_mode('mode_1_1')
    mode_1_1.inv = lambda state: state[0] >= 1 and state[0] <= 2 and state[1] >= 1 and state[1] <= 2
    mode_1_1.der = lambda _, state: [state[2], state[3], -1.2 * (state[2] - 0.7071067811865476) + 0.1 * (state[3] - -0.7071067811865475), 0.1 * (state[2] - 0.7071067811865476) + -1.2 * (state[3] - -0.7071067811865475)]
    mode_1_1.der_interval_list = [[0, 0], [0, 0], [-0.1, 0.1], [-0.1, 0.1]]

    mode_2_1 = ha.new_mode('mode_2_1')
    mode_2_1.inv = lambda state: state[0] >= 2 and state[1] >= 1 and state[1] <= 2
    mode_2_1.der = lambda _, state: [state[2], state[3], -1.2 * (state[2] - 0) + 0.1 * (state[3] - -1), 0.1 * (state[2] - 0) + -1.2 * (state[3] - -1)]
    mode_2_1.der_interval_list = [[0, 0], [0, 0], [-0.1, 0.1], [-0.1, 0.1]]

    mode_0_2 = ha.new_mode('mode_0_2')
    mode_0_2.inv = lambda state: state[0] <= 1 and state[1] >= 2
    mode_0_2.der = lambda _, state: [0, 0, 0, 0]
    mode_0_2.der_interval_list = [[0, 0], [0, 0], [0, 0], [0, 0]]

    mode_1_2 = ha.new_mode('mode_1_2')
    mode_1_2.inv = lambda state: state[0] >= 1 and state[0] <= 2 and state[1] >= 2
    mode_1_2.der = lambda _, state: [state[2], state[3], -1.2 * (state[2] - 1) + 0.1 * (state[3] - 0), 0.1 * (state[2] - 1) + -1.2 * (state[3] - 0)]
    mode_1_2.der_interval_list = [[0, 0], [0, 0], [-0.1, 0.1], [-0.1, 0.1]]

    mode_2_2 = ha.new_mode('mode_2_2')
    mode_2_2.inv = lambda state: state[0] >= 2 and state[1] >= 2
    mode_2_2.der = lambda _, state: [state[2], state[3], -1.2 * (state[2] - 0) + 0.1 * (state[3] - -1), 0.1 * (state[2] - 0) + -1.2 * (state[3] - -1)]
    mode_2_2.der_interval_list = [[0, 0], [0, 0], [-0.1, 0.1], [-0.1, 0.1]]

    t = ha.new_transition(mode_0_0, mode_1_0)
    t.guard = lambda state: state[0] >= 1
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_0_0, mode_0_1)
    t.guard = lambda state: state[1] >= 1
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_1_0, mode_0_0)
    t.guard = lambda state: state[0] <= 1
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_1_0, mode_2_0)
    t.guard = lambda state: state[0] >= 2
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_1_0, mode_1_1)
    t.guard = lambda state: state[1] >= 1
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_2_0, mode_1_0)
    t.guard = lambda state: state[0] <= 2
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_2_0, mode_2_1)
    t.guard = lambda state: state[1] >= 1
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_0_1, mode_1_1)
    t.guard = lambda state: state[0] >= 1
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_0_1, mode_0_0)
    t.guard = lambda state: state[1] <= 1
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_0_1, mode_0_2)
    t.guard = lambda state: state[1] >= 2
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_1_1, mode_0_1)
    t.guard = lambda state: state[0] <= 1
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_1_1, mode_2_1)
    t.guard = lambda state: state[0] >= 2
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_1_1, mode_1_0)
    t.guard = lambda state: state[1] <= 1
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_1_1, mode_1_2)
    t.guard = lambda state: state[1] >= 2
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_2_1, mode_1_1)
    t.guard = lambda state: state[0] <= 2
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_2_1, mode_2_0)
    t.guard = lambda state: state[1] <= 1
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_2_1, mode_2_2)
    t.guard = lambda state: state[1] >= 2
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_0_2, mode_1_2)
    t.guard = lambda state: state[0] >= 1
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_0_2, mode_0_1)
    t.guard = lambda state: state[1] <= 2
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_1_2, mode_0_2)
    t.guard = lambda state: state[0] <= 1
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_1_2, mode_2_2)
    t.guard = lambda state: state[0] >= 2
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_1_2, mode_1_1)
    t.guard = lambda state: state[1] <= 2
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_2_2, mode_1_2)
    t.guard = lambda state: state[0] <= 2
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_2_2, mode_2_1)
    t.guard = lambda state: state[1] <= 2
    t.reset = lambda state: [None, None, None, None]

    return ha

def define_init_states(ha):
    '''returns a list of (mode, HyperRectangle)'''
    # Variable ordering: [x, y, xvel, yvel]
    rv = []

    r = HyperRectangle([(0.5, 0.5), (1.5, 1.5), (-1, 1), (-1, 1)])
    rv.append((ha.modes['mode_0_1'], r))

    return rv

def define_settings():
    '''defines the automaton / plot settings'''
    s = PySimSettings()
    s.max_time = 5.0
    s.step = 0.1
    s.x_dim = 0
    s.y_dim = 1

    return s


def simulate(init_states, settings):
    '''simulate the automaton from each initial rect'''

    q_list = init_list_to_q_list(init_states, center=True, star=True, corners=True, rand=100)
    result = sim.simulate_multi(q_list, settings.max_time)

    return result

def plot(result, init_states, image_path, settings):
    '''plot a simulation result to a file'''

    draw_events = len(result) == 1
    shouldShow = False
    sim.plot_sim_result_multi(result, settings.dim_x, settings.dim_y, image_path, draw_events, legend=False, title='nav_fig1b', show=shouldShow, init_states=init_states)

if __name__ == '__main__':
    ha = define_ha()
    settings = define_settings()
    init_states = define_init_states(ha)
    plot(simulate(init_states, settings), init_states, settings)


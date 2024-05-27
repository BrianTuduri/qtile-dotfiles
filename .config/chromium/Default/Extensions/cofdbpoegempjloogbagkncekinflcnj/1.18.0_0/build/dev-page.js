(function (svelteJsoneditor) {
    'use strict';

    function noop() { }
    const identity = x => x;
    function assign(tar, src) {
        // @ts-ignore
        for (const k in src)
            tar[k] = src[k];
        return tar;
    }
    function add_location(element, file, line, column, char) {
        element.__svelte_meta = {
            loc: { file, line, column, char }
        };
    }
    function run(fn) {
        return fn();
    }
    function blank_object() {
        return Object.create(null);
    }
    function run_all(fns) {
        fns.forEach(run);
    }
    function is_function(thing) {
        return typeof thing === 'function';
    }
    function safe_not_equal(a, b) {
        return a != a ? b == b : a !== b || ((a && typeof a === 'object') || typeof a === 'function');
    }
    function is_empty(obj) {
        return Object.keys(obj).length === 0;
    }
    function validate_store(store, name) {
        if (store != null && typeof store.subscribe !== 'function') {
            throw new Error(`'${name}' is not a store with a 'subscribe' method`);
        }
    }
    function subscribe(store, ...callbacks) {
        if (store == null) {
            return noop;
        }
        const unsub = store.subscribe(...callbacks);
        return unsub.unsubscribe ? () => unsub.unsubscribe() : unsub;
    }
    function component_subscribe(component, store, callback) {
        component.$$.on_destroy.push(subscribe(store, callback));
    }
    function create_slot(definition, ctx, $$scope, fn) {
        if (definition) {
            const slot_ctx = get_slot_context(definition, ctx, $$scope, fn);
            return definition[0](slot_ctx);
        }
    }
    function get_slot_context(definition, ctx, $$scope, fn) {
        return definition[1] && fn
            ? assign($$scope.ctx.slice(), definition[1](fn(ctx)))
            : $$scope.ctx;
    }
    function get_slot_changes(definition, $$scope, dirty, fn) {
        if (definition[2] && fn) {
            const lets = definition[2](fn(dirty));
            if ($$scope.dirty === undefined) {
                return lets;
            }
            if (typeof lets === 'object') {
                const merged = [];
                const len = Math.max($$scope.dirty.length, lets.length);
                for (let i = 0; i < len; i += 1) {
                    merged[i] = $$scope.dirty[i] | lets[i];
                }
                return merged;
            }
            return $$scope.dirty | lets;
        }
        return $$scope.dirty;
    }
    function update_slot_base(slot, slot_definition, ctx, $$scope, slot_changes, get_slot_context_fn) {
        if (slot_changes) {
            const slot_context = get_slot_context(slot_definition, ctx, $$scope, get_slot_context_fn);
            slot.p(slot_context, slot_changes);
        }
    }
    function get_all_dirty_from_scope($$scope) {
        if ($$scope.ctx.length > 32) {
            const dirty = [];
            const length = $$scope.ctx.length / 32;
            for (let i = 0; i < length; i++) {
                dirty[i] = -1;
            }
            return dirty;
        }
        return -1;
    }
    function null_to_empty(value) {
        return value == null ? '' : value;
    }

    const is_client = typeof window !== 'undefined';
    let now = is_client
        ? () => window.performance.now()
        : () => Date.now();
    let raf = is_client ? cb => requestAnimationFrame(cb) : noop;

    const tasks = new Set();
    function run_tasks(now) {
        tasks.forEach(task => {
            if (!task.c(now)) {
                tasks.delete(task);
                task.f();
            }
        });
        if (tasks.size !== 0)
            raf(run_tasks);
    }
    /**
     * Creates a new task that runs on each raf frame
     * until it returns a falsy value or is aborted
     */
    function loop(callback) {
        let task;
        if (tasks.size === 0)
            raf(run_tasks);
        return {
            promise: new Promise(fulfill => {
                tasks.add(task = { c: callback, f: fulfill });
            }),
            abort() {
                tasks.delete(task);
            }
        };
    }

    const globals = (typeof window !== 'undefined'
        ? window
        : typeof globalThis !== 'undefined'
            ? globalThis
            : global);
    function append(target, node) {
        target.appendChild(node);
    }
    function get_root_for_style(node) {
        if (!node)
            return document;
        const root = node.getRootNode ? node.getRootNode() : node.ownerDocument;
        if (root && root.host) {
            return root;
        }
        return node.ownerDocument;
    }
    function append_empty_stylesheet(node) {
        const style_element = element('style');
        append_stylesheet(get_root_for_style(node), style_element);
        return style_element.sheet;
    }
    function append_stylesheet(node, style) {
        append(node.head || node, style);
        return style.sheet;
    }
    function insert(target, node, anchor) {
        target.insertBefore(node, anchor || null);
    }
    function detach(node) {
        if (node.parentNode) {
            node.parentNode.removeChild(node);
        }
    }
    function element(name) {
        return document.createElement(name);
    }
    function text(data) {
        return document.createTextNode(data);
    }
    function space() {
        return text(' ');
    }
    function listen(node, event, handler, options) {
        node.addEventListener(event, handler, options);
        return () => node.removeEventListener(event, handler, options);
    }
    function attr(node, attribute, value) {
        if (value == null)
            node.removeAttribute(attribute);
        else if (node.getAttribute(attribute) !== value)
            node.setAttribute(attribute, value);
    }
    function children(element) {
        return Array.from(element.childNodes);
    }
    function custom_event(type, detail, { bubbles = false, cancelable = false } = {}) {
        const e = document.createEvent('CustomEvent');
        e.initCustomEvent(type, bubbles, cancelable, detail);
        return e;
    }

    // we need to store the information for multiple documents because a Svelte application could also contain iframes
    // https://github.com/sveltejs/svelte/issues/3624
    const managed_styles = new Map();
    let active = 0;
    // https://github.com/darkskyapp/string-hash/blob/master/index.js
    function hash(str) {
        let hash = 5381;
        let i = str.length;
        while (i--)
            hash = ((hash << 5) - hash) ^ str.charCodeAt(i);
        return hash >>> 0;
    }
    function create_style_information(doc, node) {
        const info = { stylesheet: append_empty_stylesheet(node), rules: {} };
        managed_styles.set(doc, info);
        return info;
    }
    function create_rule(node, a, b, duration, delay, ease, fn, uid = 0) {
        const step = 16.666 / duration;
        let keyframes = '{\n';
        for (let p = 0; p <= 1; p += step) {
            const t = a + (b - a) * ease(p);
            keyframes += p * 100 + `%{${fn(t, 1 - t)}}\n`;
        }
        const rule = keyframes + `100% {${fn(b, 1 - b)}}\n}`;
        const name = `__svelte_${hash(rule)}_${uid}`;
        const doc = get_root_for_style(node);
        const { stylesheet, rules } = managed_styles.get(doc) || create_style_information(doc, node);
        if (!rules[name]) {
            rules[name] = true;
            stylesheet.insertRule(`@keyframes ${name} ${rule}`, stylesheet.cssRules.length);
        }
        const animation = node.style.animation || '';
        node.style.animation = `${animation ? `${animation}, ` : ''}${name} ${duration}ms linear ${delay}ms 1 both`;
        active += 1;
        return name;
    }
    function delete_rule(node, name) {
        const previous = (node.style.animation || '').split(', ');
        const next = previous.filter(name
            ? anim => anim.indexOf(name) < 0 // remove specific animation
            : anim => anim.indexOf('__svelte') === -1 // remove all Svelte animations
        );
        const deleted = previous.length - next.length;
        if (deleted) {
            node.style.animation = next.join(', ');
            active -= deleted;
            if (!active)
                clear_rules();
        }
    }
    function clear_rules() {
        raf(() => {
            if (active)
                return;
            managed_styles.forEach(info => {
                const { ownerNode } = info.stylesheet;
                // there is no ownerNode if it runs on jsdom.
                if (ownerNode)
                    detach(ownerNode);
            });
            managed_styles.clear();
        });
    }

    let current_component;
    function set_current_component(component) {
        current_component = component;
    }
    function get_current_component() {
        if (!current_component)
            throw new Error('Function called outside component initialization');
        return current_component;
    }
    /**
     * The `onMount` function schedules a callback to run as soon as the component has been mounted to the DOM.
     * It must be called during the component's initialisation (but doesn't need to live *inside* the component;
     * it can be called from an external module).
     *
     * `onMount` does not run inside a [server-side component](/docs#run-time-server-side-component-api).
     *
     * https://svelte.dev/docs#run-time-svelte-onmount
     */
    function onMount(fn) {
        get_current_component().$$.on_mount.push(fn);
    }
    /**
     * Associates an arbitrary `context` object with the current component and the specified `key`
     * and returns that object. The context is then available to children of the component
     * (including slotted content) with `getContext`.
     *
     * Like lifecycle functions, this must be called during component initialisation.
     *
     * https://svelte.dev/docs#run-time-svelte-setcontext
     */
    function setContext(key, context) {
        get_current_component().$$.context.set(key, context);
        return context;
    }

    const dirty_components = [];
    const binding_callbacks = [];
    let render_callbacks = [];
    const flush_callbacks = [];
    const resolved_promise = /* @__PURE__ */ Promise.resolve();
    let update_scheduled = false;
    function schedule_update() {
        if (!update_scheduled) {
            update_scheduled = true;
            resolved_promise.then(flush);
        }
    }
    function add_render_callback(fn) {
        render_callbacks.push(fn);
    }
    // flush() calls callbacks in this order:
    // 1. All beforeUpdate callbacks, in order: parents before children
    // 2. All bind:this callbacks, in reverse order: children before parents.
    // 3. All afterUpdate callbacks, in order: parents before children. EXCEPT
    //    for afterUpdates called during the initial onMount, which are called in
    //    reverse order: children before parents.
    // Since callbacks might update component values, which could trigger another
    // call to flush(), the following steps guard against this:
    // 1. During beforeUpdate, any updated components will be added to the
    //    dirty_components array and will cause a reentrant call to flush(). Because
    //    the flush index is kept outside the function, the reentrant call will pick
    //    up where the earlier call left off and go through all dirty components. The
    //    current_component value is saved and restored so that the reentrant call will
    //    not interfere with the "parent" flush() call.
    // 2. bind:this callbacks cannot trigger new flush() calls.
    // 3. During afterUpdate, any updated components will NOT have their afterUpdate
    //    callback called a second time; the seen_callbacks set, outside the flush()
    //    function, guarantees this behavior.
    const seen_callbacks = new Set();
    let flushidx = 0; // Do *not* move this inside the flush() function
    function flush() {
        // Do not reenter flush while dirty components are updated, as this can
        // result in an infinite loop. Instead, let the inner flush handle it.
        // Reentrancy is ok afterwards for bindings etc.
        if (flushidx !== 0) {
            return;
        }
        const saved_component = current_component;
        do {
            // first, call beforeUpdate functions
            // and update components
            try {
                while (flushidx < dirty_components.length) {
                    const component = dirty_components[flushidx];
                    flushidx++;
                    set_current_component(component);
                    update(component.$$);
                }
            }
            catch (e) {
                // reset dirty state to not end up in a deadlocked state and then rethrow
                dirty_components.length = 0;
                flushidx = 0;
                throw e;
            }
            set_current_component(null);
            dirty_components.length = 0;
            flushidx = 0;
            while (binding_callbacks.length)
                binding_callbacks.pop()();
            // then, once components are updated, call
            // afterUpdate functions. This may cause
            // subsequent updates...
            for (let i = 0; i < render_callbacks.length; i += 1) {
                const callback = render_callbacks[i];
                if (!seen_callbacks.has(callback)) {
                    // ...so guard against infinite loops
                    seen_callbacks.add(callback);
                    callback();
                }
            }
            render_callbacks.length = 0;
        } while (dirty_components.length);
        while (flush_callbacks.length) {
            flush_callbacks.pop()();
        }
        update_scheduled = false;
        seen_callbacks.clear();
        set_current_component(saved_component);
    }
    function update($$) {
        if ($$.fragment !== null) {
            $$.update();
            run_all($$.before_update);
            const dirty = $$.dirty;
            $$.dirty = [-1];
            $$.fragment && $$.fragment.p($$.ctx, dirty);
            $$.after_update.forEach(add_render_callback);
        }
    }
    /**
     * Useful for example to execute remaining `afterUpdate` callbacks before executing `destroy`.
     */
    function flush_render_callbacks(fns) {
        const filtered = [];
        const targets = [];
        render_callbacks.forEach((c) => fns.indexOf(c) === -1 ? filtered.push(c) : targets.push(c));
        targets.forEach((c) => c());
        render_callbacks = filtered;
    }

    let promise;
    function wait() {
        if (!promise) {
            promise = Promise.resolve();
            promise.then(() => {
                promise = null;
            });
        }
        return promise;
    }
    function dispatch(node, direction, kind) {
        node.dispatchEvent(custom_event(`${direction ? 'intro' : 'outro'}${kind}`));
    }
    const outroing = new Set();
    let outros;
    function group_outros() {
        outros = {
            r: 0,
            c: [],
            p: outros // parent group
        };
    }
    function check_outros() {
        if (!outros.r) {
            run_all(outros.c);
        }
        outros = outros.p;
    }
    function transition_in(block, local) {
        if (block && block.i) {
            outroing.delete(block);
            block.i(local);
        }
    }
    function transition_out(block, local, detach, callback) {
        if (block && block.o) {
            if (outroing.has(block))
                return;
            outroing.add(block);
            outros.c.push(() => {
                outroing.delete(block);
                if (callback) {
                    if (detach)
                        block.d(1);
                    callback();
                }
            });
            block.o(local);
        }
        else if (callback) {
            callback();
        }
    }
    const null_transition = { duration: 0 };
    function create_bidirectional_transition(node, fn, params, intro) {
        const options = { direction: 'both' };
        let config = fn(node, params, options);
        let t = intro ? 0 : 1;
        let running_program = null;
        let pending_program = null;
        let animation_name = null;
        function clear_animation() {
            if (animation_name)
                delete_rule(node, animation_name);
        }
        function init(program, duration) {
            const d = (program.b - t);
            duration *= Math.abs(d);
            return {
                a: t,
                b: program.b,
                d,
                duration,
                start: program.start,
                end: program.start + duration,
                group: program.group
            };
        }
        function go(b) {
            const { delay = 0, duration = 300, easing = identity, tick = noop, css } = config || null_transition;
            const program = {
                start: now() + delay,
                b
            };
            if (!b) {
                // @ts-ignore todo: improve typings
                program.group = outros;
                outros.r += 1;
            }
            if (running_program || pending_program) {
                pending_program = program;
            }
            else {
                // if this is an intro, and there's a delay, we need to do
                // an initial tick and/or apply CSS animation immediately
                if (css) {
                    clear_animation();
                    animation_name = create_rule(node, t, b, duration, delay, easing, css);
                }
                if (b)
                    tick(0, 1);
                running_program = init(program, duration);
                add_render_callback(() => dispatch(node, b, 'start'));
                loop(now => {
                    if (pending_program && now > pending_program.start) {
                        running_program = init(pending_program, duration);
                        pending_program = null;
                        dispatch(node, running_program.b, 'start');
                        if (css) {
                            clear_animation();
                            animation_name = create_rule(node, t, running_program.b, running_program.duration, 0, easing, config.css);
                        }
                    }
                    if (running_program) {
                        if (now >= running_program.end) {
                            tick(t = running_program.b, 1 - t);
                            dispatch(node, running_program.b, 'end');
                            if (!pending_program) {
                                // we're done
                                if (running_program.b) {
                                    // intro — we can tidy up immediately
                                    clear_animation();
                                }
                                else {
                                    // outro — needs to be coordinated
                                    if (!--running_program.group.r)
                                        run_all(running_program.group.c);
                                }
                            }
                            running_program = null;
                        }
                        else if (now >= running_program.start) {
                            const p = now - running_program.start;
                            t = running_program.a + running_program.d * easing(p / running_program.duration);
                            tick(t, 1 - t);
                        }
                    }
                    return !!(running_program || pending_program);
                });
            }
        }
        return {
            run(b) {
                if (is_function(config)) {
                    wait().then(() => {
                        // @ts-ignore
                        config = config(options);
                        go(b);
                    });
                }
                else {
                    go(b);
                }
            },
            end() {
                clear_animation();
                running_program = pending_program = null;
            }
        };
    }
    function create_component(block) {
        block && block.c();
    }
    function mount_component(component, target, anchor, customElement) {
        const { fragment, after_update } = component.$$;
        fragment && fragment.m(target, anchor);
        if (!customElement) {
            // onMount happens before the initial afterUpdate
            add_render_callback(() => {
                const new_on_destroy = component.$$.on_mount.map(run).filter(is_function);
                // if the component was destroyed immediately
                // it will update the `$$.on_destroy` reference to `null`.
                // the destructured on_destroy may still reference to the old array
                if (component.$$.on_destroy) {
                    component.$$.on_destroy.push(...new_on_destroy);
                }
                else {
                    // Edge case - component was destroyed immediately,
                    // most likely as a result of a binding initialising
                    run_all(new_on_destroy);
                }
                component.$$.on_mount = [];
            });
        }
        after_update.forEach(add_render_callback);
    }
    function destroy_component(component, detaching) {
        const $$ = component.$$;
        if ($$.fragment !== null) {
            flush_render_callbacks($$.after_update);
            run_all($$.on_destroy);
            $$.fragment && $$.fragment.d(detaching);
            // TODO null out other refs, including component.$$ (but need to
            // preserve final state?)
            $$.on_destroy = $$.fragment = null;
            $$.ctx = [];
        }
    }
    function make_dirty(component, i) {
        if (component.$$.dirty[0] === -1) {
            dirty_components.push(component);
            schedule_update();
            component.$$.dirty.fill(0);
        }
        component.$$.dirty[(i / 31) | 0] |= (1 << (i % 31));
    }
    function init(component, options, instance, create_fragment, not_equal, props, append_styles, dirty = [-1]) {
        const parent_component = current_component;
        set_current_component(component);
        const $$ = component.$$ = {
            fragment: null,
            ctx: [],
            // state
            props,
            update: noop,
            not_equal,
            bound: blank_object(),
            // lifecycle
            on_mount: [],
            on_destroy: [],
            on_disconnect: [],
            before_update: [],
            after_update: [],
            context: new Map(options.context || (parent_component ? parent_component.$$.context : [])),
            // everything else
            callbacks: blank_object(),
            dirty,
            skip_bound: false,
            root: options.target || parent_component.$$.root
        };
        append_styles && append_styles($$.root);
        let ready = false;
        $$.ctx = instance
            ? instance(component, options.props || {}, (i, ret, ...rest) => {
                const value = rest.length ? rest[0] : ret;
                if ($$.ctx && not_equal($$.ctx[i], $$.ctx[i] = value)) {
                    if (!$$.skip_bound && $$.bound[i])
                        $$.bound[i](value);
                    if (ready)
                        make_dirty(component, i);
                }
                return ret;
            })
            : [];
        $$.update();
        ready = true;
        run_all($$.before_update);
        // `false` as a special case of no DOM component
        $$.fragment = create_fragment ? create_fragment($$.ctx) : false;
        if (options.target) {
            if (options.hydrate) {
                const nodes = children(options.target);
                // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
                $$.fragment && $$.fragment.l(nodes);
                nodes.forEach(detach);
            }
            else {
                // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
                $$.fragment && $$.fragment.c();
            }
            if (options.intro)
                transition_in(component.$$.fragment);
            mount_component(component, options.target, options.anchor, options.customElement);
            flush();
        }
        set_current_component(parent_component);
    }
    /**
     * Base class for Svelte components. Used when dev=false.
     */
    class SvelteComponent {
        $destroy() {
            destroy_component(this, 1);
            this.$destroy = noop;
        }
        $on(type, callback) {
            if (!is_function(callback)) {
                return noop;
            }
            const callbacks = (this.$$.callbacks[type] || (this.$$.callbacks[type] = []));
            callbacks.push(callback);
            return () => {
                const index = callbacks.indexOf(callback);
                if (index !== -1)
                    callbacks.splice(index, 1);
            };
        }
        $set($$props) {
            if (this.$$set && !is_empty($$props)) {
                this.$$.skip_bound = true;
                this.$$set($$props);
                this.$$.skip_bound = false;
            }
        }
    }

    function dispatch_dev(type, detail) {
        document.dispatchEvent(custom_event(type, Object.assign({ version: '3.59.2' }, detail), { bubbles: true }));
    }
    function append_dev(target, node) {
        dispatch_dev('SvelteDOMInsert', { target, node });
        append(target, node);
    }
    function insert_dev(target, node, anchor) {
        dispatch_dev('SvelteDOMInsert', { target, node, anchor });
        insert(target, node, anchor);
    }
    function detach_dev(node) {
        dispatch_dev('SvelteDOMRemove', { node });
        detach(node);
    }
    function listen_dev(node, event, handler, options, has_prevent_default, has_stop_propagation, has_stop_immediate_propagation) {
        const modifiers = options === true ? ['capture'] : options ? Array.from(Object.keys(options)) : [];
        if (has_prevent_default)
            modifiers.push('preventDefault');
        if (has_stop_propagation)
            modifiers.push('stopPropagation');
        if (has_stop_immediate_propagation)
            modifiers.push('stopImmediatePropagation');
        dispatch_dev('SvelteDOMAddEventListener', { node, event, handler, modifiers });
        const dispose = listen(node, event, handler, options);
        return () => {
            dispatch_dev('SvelteDOMRemoveEventListener', { node, event, handler, modifiers });
            dispose();
        };
    }
    function attr_dev(node, attribute, value) {
        attr(node, attribute, value);
        if (value == null)
            dispatch_dev('SvelteDOMRemoveAttribute', { node, attribute });
        else
            dispatch_dev('SvelteDOMSetAttribute', { node, attribute, value });
    }
    function prop_dev(node, property, value) {
        node[property] = value;
        dispatch_dev('SvelteDOMSetProperty', { node, property, value });
    }
    function set_data_dev(text, data) {
        data = '' + data;
        if (text.data === data)
            return;
        dispatch_dev('SvelteDOMSetData', { node: text, data });
        text.data = data;
    }
    function validate_slots(name, slot, keys) {
        for (const slot_key of Object.keys(slot)) {
            if (!~keys.indexOf(slot_key)) {
                console.warn(`<${name}> received an unexpected slot "${slot_key}".`);
            }
        }
    }
    /**
     * Base class for Svelte components with some minor dev-enhancements. Used when dev=true.
     */
    class SvelteComponentDev extends SvelteComponent {
        constructor(options) {
            if (!options || (!options.target && !options.$$inline)) {
                throw new Error("'target' is a required option");
            }
            super();
        }
        $destroy() {
            super.$destroy();
            this.$destroy = () => {
                console.warn('Component was already destroyed'); // eslint-disable-line no-console
            };
        }
        $capture_state() { }
        $inject_state() { }
    }

    /* src/common/components/accordion/Accordion.svelte generated by Svelte v3.59.2 */

    const file$3 = "src/common/components/accordion/Accordion.svelte";

    function create_fragment$3(ctx) {
    	let div;
    	let current;
    	const default_slot_template = /*#slots*/ ctx[1].default;
    	const default_slot = create_slot(default_slot_template, ctx, /*$$scope*/ ctx[0], null);

    	const block = {
    		c: function create() {
    			div = element("div");
    			if (default_slot) default_slot.c();
    			attr_dev(div, "class", "container");
    			add_location(div, file$3, 3, 0, 20);
    		},
    		l: function claim(nodes) {
    			throw new Error("options.hydrate only works if the component was compiled with the `hydratable: true` option");
    		},
    		m: function mount(target, anchor) {
    			insert_dev(target, div, anchor);

    			if (default_slot) {
    				default_slot.m(div, null);
    			}

    			current = true;
    		},
    		p: function update(ctx, [dirty]) {
    			if (default_slot) {
    				if (default_slot.p && (!current || dirty & /*$$scope*/ 1)) {
    					update_slot_base(
    						default_slot,
    						default_slot_template,
    						ctx,
    						/*$$scope*/ ctx[0],
    						!current
    						? get_all_dirty_from_scope(/*$$scope*/ ctx[0])
    						: get_slot_changes(default_slot_template, /*$$scope*/ ctx[0], dirty, null),
    						null
    					);
    				}
    			}
    		},
    		i: function intro(local) {
    			if (current) return;
    			transition_in(default_slot, local);
    			current = true;
    		},
    		o: function outro(local) {
    			transition_out(default_slot, local);
    			current = false;
    		},
    		d: function destroy(detaching) {
    			if (detaching) detach_dev(div);
    			if (default_slot) default_slot.d(detaching);
    		}
    	};

    	dispatch_dev("SvelteRegisterBlock", {
    		block,
    		id: create_fragment$3.name,
    		type: "component",
    		source: "",
    		ctx
    	});

    	return block;
    }

    function instance$3($$self, $$props, $$invalidate) {
    	let { $$slots: slots = {}, $$scope } = $$props;
    	validate_slots('Accordion', slots, ['default']);
    	const writable_props = [];

    	Object.keys($$props).forEach(key => {
    		if (!~writable_props.indexOf(key) && key.slice(0, 2) !== '$$' && key !== 'slot') console.warn(`<Accordion> was created with unknown prop '${key}'`);
    	});

    	$$self.$$set = $$props => {
    		if ('$$scope' in $$props) $$invalidate(0, $$scope = $$props.$$scope);
    	};

    	return [$$scope, slots];
    }

    class Accordion extends SvelteComponentDev {
    	constructor(options) {
    		super(options);
    		init(this, options, instance$3, create_fragment$3, safe_not_equal, {});

    		dispatch_dev("SvelteRegisterComponent", {
    			component: this,
    			tagName: "Accordion",
    			options,
    			id: create_fragment$3.name
    		});
    	}
    }

    function cubicOut(t) {
        const f = t - 1.0;
        return f * f * f + 1.0;
    }

    function slide(node, { delay = 0, duration = 400, easing = cubicOut, axis = 'y' } = {}) {
        const style = getComputedStyle(node);
        const opacity = +style.opacity;
        const primary_property = axis === 'y' ? 'height' : 'width';
        const primary_property_value = parseFloat(style[primary_property]);
        const secondary_properties = axis === 'y' ? ['top', 'bottom'] : ['left', 'right'];
        const capitalized_secondary_properties = secondary_properties.map((e) => `${e[0].toUpperCase()}${e.slice(1)}`);
        const padding_start_value = parseFloat(style[`padding${capitalized_secondary_properties[0]}`]);
        const padding_end_value = parseFloat(style[`padding${capitalized_secondary_properties[1]}`]);
        const margin_start_value = parseFloat(style[`margin${capitalized_secondary_properties[0]}`]);
        const margin_end_value = parseFloat(style[`margin${capitalized_secondary_properties[1]}`]);
        const border_width_start_value = parseFloat(style[`border${capitalized_secondary_properties[0]}Width`]);
        const border_width_end_value = parseFloat(style[`border${capitalized_secondary_properties[1]}Width`]);
        return {
            delay,
            duration,
            easing,
            css: t => 'overflow: hidden;' +
                `opacity: ${Math.min(t * 20, 1) * opacity};` +
                `${primary_property}: ${t * primary_property_value}px;` +
                `padding-${secondary_properties[0]}: ${t * padding_start_value}px;` +
                `padding-${secondary_properties[1]}: ${t * padding_end_value}px;` +
                `margin-${secondary_properties[0]}: ${t * margin_start_value}px;` +
                `margin-${secondary_properties[1]}: ${t * margin_end_value}px;` +
                `border-${secondary_properties[0]}-width: ${t * border_width_start_value}px;` +
                `border-${secondary_properties[1]}-width: ${t * border_width_end_value}px;`
        };
    }

    /* src/common/components/accordion/AccordionSection.svelte generated by Svelte v3.59.2 */
    const file$2 = "src/common/components/accordion/AccordionSection.svelte";

    // (13:2) {#if showContent}
    function create_if_block(ctx) {
    	let div;
    	let div_transition;
    	let current;
    	const default_slot_template = /*#slots*/ ctx[3].default;
    	const default_slot = create_slot(default_slot_template, ctx, /*$$scope*/ ctx[2], null);

    	const block = {
    		c: function create() {
    			div = element("div");
    			if (default_slot) default_slot.c();
    			attr_dev(div, "class", "content svelte-i8de7y");
    			add_location(div, file$2, 13, 4, 269);
    		},
    		m: function mount(target, anchor) {
    			insert_dev(target, div, anchor);

    			if (default_slot) {
    				default_slot.m(div, null);
    			}

    			current = true;
    		},
    		p: function update(ctx, dirty) {
    			if (default_slot) {
    				if (default_slot.p && (!current || dirty & /*$$scope*/ 4)) {
    					update_slot_base(
    						default_slot,
    						default_slot_template,
    						ctx,
    						/*$$scope*/ ctx[2],
    						!current
    						? get_all_dirty_from_scope(/*$$scope*/ ctx[2])
    						: get_slot_changes(default_slot_template, /*$$scope*/ ctx[2], dirty, null),
    						null
    					);
    				}
    			}
    		},
    		i: function intro(local) {
    			if (current) return;
    			transition_in(default_slot, local);

    			if (local) {
    				add_render_callback(() => {
    					if (!current) return;
    					if (!div_transition) div_transition = create_bidirectional_transition(div, slide, {}, true);
    					div_transition.run(1);
    				});
    			}

    			current = true;
    		},
    		o: function outro(local) {
    			transition_out(default_slot, local);

    			if (local) {
    				if (!div_transition) div_transition = create_bidirectional_transition(div, slide, {}, false);
    				div_transition.run(0);
    			}

    			current = false;
    		},
    		d: function destroy(detaching) {
    			if (detaching) detach_dev(div);
    			if (default_slot) default_slot.d(detaching);
    			if (detaching && div_transition) div_transition.end();
    		}
    	};

    	dispatch_dev("SvelteRegisterBlock", {
    		block,
    		id: create_if_block.name,
    		type: "if",
    		source: "(13:2) {#if showContent}",
    		ctx
    	});

    	return block;
    }

    function create_fragment$2(ctx) {
    	let div;
    	let button;
    	let t0;
    	let button_class_value;
    	let t1;
    	let current;
    	let mounted;
    	let dispose;
    	let if_block = /*showContent*/ ctx[1] && create_if_block(ctx);

    	const block = {
    		c: function create() {
    			div = element("div");
    			button = element("button");
    			t0 = text(/*headline*/ ctx[0]);
    			t1 = space();
    			if (if_block) if_block.c();
    			attr_dev(button, "class", button_class_value = "" + (null_to_empty(/*showContent*/ ctx[1] ? "close" : "open") + " svelte-i8de7y"));
    			add_location(button, file$2, 8, 2, 121);
    			add_location(div, file$2, 7, 0, 113);
    		},
    		l: function claim(nodes) {
    			throw new Error("options.hydrate only works if the component was compiled with the `hydratable: true` option");
    		},
    		m: function mount(target, anchor) {
    			insert_dev(target, div, anchor);
    			append_dev(div, button);
    			append_dev(button, t0);
    			append_dev(div, t1);
    			if (if_block) if_block.m(div, null);
    			current = true;

    			if (!mounted) {
    				dispose = listen_dev(button, "click", /*click_handler*/ ctx[4], false, false, false, false);
    				mounted = true;
    			}
    		},
    		p: function update(ctx, [dirty]) {
    			if (!current || dirty & /*headline*/ 1) set_data_dev(t0, /*headline*/ ctx[0]);

    			if (!current || dirty & /*showContent*/ 2 && button_class_value !== (button_class_value = "" + (null_to_empty(/*showContent*/ ctx[1] ? "close" : "open") + " svelte-i8de7y"))) {
    				attr_dev(button, "class", button_class_value);
    			}

    			if (/*showContent*/ ctx[1]) {
    				if (if_block) {
    					if_block.p(ctx, dirty);

    					if (dirty & /*showContent*/ 2) {
    						transition_in(if_block, 1);
    					}
    				} else {
    					if_block = create_if_block(ctx);
    					if_block.c();
    					transition_in(if_block, 1);
    					if_block.m(div, null);
    				}
    			} else if (if_block) {
    				group_outros();

    				transition_out(if_block, 1, 1, () => {
    					if_block = null;
    				});

    				check_outros();
    			}
    		},
    		i: function intro(local) {
    			if (current) return;
    			transition_in(if_block);
    			current = true;
    		},
    		o: function outro(local) {
    			transition_out(if_block);
    			current = false;
    		},
    		d: function destroy(detaching) {
    			if (detaching) detach_dev(div);
    			if (if_block) if_block.d();
    			mounted = false;
    			dispose();
    		}
    	};

    	dispatch_dev("SvelteRegisterBlock", {
    		block,
    		id: create_fragment$2.name,
    		type: "component",
    		source: "",
    		ctx
    	});

    	return block;
    }

    function instance$2($$self, $$props, $$invalidate) {
    	let { $$slots: slots = {}, $$scope } = $$props;
    	validate_slots('AccordionSection', slots, ['default']);
    	let { headline } = $$props;
    	let showContent = false;

    	$$self.$$.on_mount.push(function () {
    		if (headline === undefined && !('headline' in $$props || $$self.$$.bound[$$self.$$.props['headline']])) {
    			console.warn("<AccordionSection> was created without expected prop 'headline'");
    		}
    	});

    	const writable_props = ['headline'];

    	Object.keys($$props).forEach(key => {
    		if (!~writable_props.indexOf(key) && key.slice(0, 2) !== '$$' && key !== 'slot') console.warn(`<AccordionSection> was created with unknown prop '${key}'`);
    	});

    	const click_handler = () => $$invalidate(1, showContent = !showContent);

    	$$self.$$set = $$props => {
    		if ('headline' in $$props) $$invalidate(0, headline = $$props.headline);
    		if ('$$scope' in $$props) $$invalidate(2, $$scope = $$props.$$scope);
    	};

    	$$self.$capture_state = () => ({ slide, headline, showContent });

    	$$self.$inject_state = $$props => {
    		if ('headline' in $$props) $$invalidate(0, headline = $$props.headline);
    		if ('showContent' in $$props) $$invalidate(1, showContent = $$props.showContent);
    	};

    	if ($$props && "$$inject" in $$props) {
    		$$self.$inject_state($$props.$$inject);
    	}

    	return [headline, showContent, $$scope, slots, click_handler];
    }

    class AccordionSection extends SvelteComponentDev {
    	constructor(options) {
    		super(options);
    		init(this, options, instance$2, create_fragment$2, safe_not_equal, { headline: 0 });

    		dispatch_dev("SvelteRegisterComponent", {
    			component: this,
    			tagName: "AccordionSection",
    			options,
    			id: create_fragment$2.name
    		});
    	}

    	get headline() {
    		throw new Error("<AccordionSection>: Props cannot be read directly from the component instance unless compiling with 'accessors: true' or '<svelte:options accessors/>'");
    	}

    	set headline(value) {
    		throw new Error("<AccordionSection>: Props cannot be set directly on the component instance unless compiling with 'accessors: true' or '<svelte:options accessors/>'");
    	}
    }

    function sendAction(action) {
      return new Promise((resolve, reject) => {
        chrome.runtime.sendMessage(action, function (response) {
          if (response && response.error) {
            reject(response);
          }
          resolve(response);
        });
      })
    }

    /* src/devPage/ClassicApiTest.svelte generated by Svelte v3.59.2 */
    const file$1 = "src/devPage/ClassicApiTest.svelte";

    function create_fragment$1(ctx) {
    	let div3;
    	let div0;
    	let textarea;
    	let t0;
    	let div1;
    	let button;
    	let t2;
    	let div2;
    	let pre;
    	let t3_value = JSON.stringify(/*result*/ ctx[0], null, 2) + "";
    	let t3;
    	let mounted;
    	let dispose;

    	const block = {
    		c: function create() {
    			div3 = element("div");
    			div0 = element("div");
    			textarea = element("textarea");
    			t0 = space();
    			div1 = element("div");
    			button = element("button");
    			button.textContent = "Submit";
    			t2 = space();
    			div2 = element("div");
    			pre = element("pre");
    			t3 = text(t3_value);
    			attr_dev(textarea, "id", "dl-textarea-info");
    			add_location(textarea, file$1, 18, 4, 334);
    			add_location(div0, file$1, 17, 2, 324);
    			add_location(button, file$1, 21, 4, 390);
    			add_location(div1, file$1, 20, 2, 380);
    			add_location(pre, file$1, 24, 4, 461);
    			add_location(div2, file$1, 23, 2, 451);
    			add_location(div3, file$1, 16, 0, 316);
    		},
    		l: function claim(nodes) {
    			throw new Error("options.hydrate only works if the component was compiled with the `hydratable: true` option");
    		},
    		m: function mount(target, anchor) {
    			insert_dev(target, div3, anchor);
    			append_dev(div3, div0);
    			append_dev(div0, textarea);
    			append_dev(div3, t0);
    			append_dev(div3, div1);
    			append_dev(div1, button);
    			append_dev(div3, t2);
    			append_dev(div3, div2);
    			append_dev(div2, pre);
    			append_dev(pre, t3);

    			if (!mounted) {
    				dispose = listen_dev(button, "click", /*testClassicApi*/ ctx[1], false, false, false, false);
    				mounted = true;
    			}
    		},
    		p: function update(ctx, [dirty]) {
    			if (dirty & /*result*/ 1 && t3_value !== (t3_value = JSON.stringify(/*result*/ ctx[0], null, 2) + "")) set_data_dev(t3, t3_value);
    		},
    		i: noop,
    		o: noop,
    		d: function destroy(detaching) {
    			if (detaching) detach_dev(div3);
    			mounted = false;
    			dispose();
    		}
    	};

    	dispatch_dev("SvelteRegisterBlock", {
    		block,
    		id: create_fragment$1.name,
    		type: "component",
    		source: "",
    		ctx
    	});

    	return block;
    }

    function instance$1($$self, $$props, $$invalidate) {
    	let { $$slots: slots = {}, $$scope } = $$props;
    	validate_slots('ClassicApiTest', slots, []);
    	let result = {};

    	async function testClassicApi() {
    		const text = document.querySelector("#dl-textarea-info").value;

    		$$invalidate(0, result = await sendAction({
    			action: "dlTestClassicAPI",
    			payload: { text }
    		}));
    	}

    	const writable_props = [];

    	Object.keys($$props).forEach(key => {
    		if (!~writable_props.indexOf(key) && key.slice(0, 2) !== '$$' && key !== 'slot') console.warn(`<ClassicApiTest> was created with unknown prop '${key}'`);
    	});

    	$$self.$capture_state = () => ({ sendAction, result, testClassicApi });

    	$$self.$inject_state = $$props => {
    		if ('result' in $$props) $$invalidate(0, result = $$props.result);
    	};

    	if ($$props && "$$inject" in $$props) {
    		$$self.$inject_state($$props.$$inject);
    	}

    	return [result, testClassicApi];
    }

    class ClassicApiTest extends SvelteComponentDev {
    	constructor(options) {
    		super(options);
    		init(this, options, instance$1, create_fragment$1, safe_not_equal, {});

    		dispatch_dev("SvelteRegisterComponent", {
    			component: this,
    			tagName: "ClassicApiTest",
    			options,
    			id: create_fragment$1.name
    		});
    	}
    }

    /**
     * This function checks whether the direction of a given language should be "rtl" or "ltr".
     * The parameter "lang" is useful for setting the direction of, let's say, the Inline Translation result.
     * If the parameter "lang" is left empty, it will take the UI language (useful for the UX).
     *
     * Also, Arabic, Hebrew, Persian and Urdu are included already in this function, so we are covered for some time.
     *
     * @param {string} lang
     * @returns string ("rtl"|"ltr")
     */
    const getTextDirection = (lang) => {
      const rtlLanguages = ["ar", "he", "fa", "ur"];
      const languageToCheck = lang || chrome.i18n.getUILanguage();

      return rtlLanguages.includes(languageToCheck.toLowerCase().substr(0, 2))
        ? "rtl"
        : "ltr"
    };

    const subscriber_queue = [];
    /**
     * Creates a `Readable` store that allows reading by subscription.
     * @param value initial value
     * @param {StartStopNotifier} [start]
     */
    function readable(value, start) {
        return {
            subscribe: writable(value, start).subscribe
        };
    }
    /**
     * Create a `Writable` store that allows both updating and reading by subscription.
     * @param {*=}value initial value
     * @param {StartStopNotifier=} start
     */
    function writable(value, start = noop) {
        let stop;
        const subscribers = new Set();
        function set(new_value) {
            if (safe_not_equal(value, new_value)) {
                value = new_value;
                if (stop) { // store is ready
                    const run_queue = !subscriber_queue.length;
                    for (const subscriber of subscribers) {
                        subscriber[1]();
                        subscriber_queue.push(subscriber, value);
                    }
                    if (run_queue) {
                        for (let i = 0; i < subscriber_queue.length; i += 2) {
                            subscriber_queue[i][0](subscriber_queue[i + 1]);
                        }
                        subscriber_queue.length = 0;
                    }
                }
            }
        }
        function update(fn) {
            set(fn(value));
        }
        function subscribe(run, invalidate = noop) {
            const subscriber = [run, invalidate];
            subscribers.add(subscriber);
            if (subscribers.size === 1) {
                stop = start(set) || noop;
            }
            run(value);
            return () => {
                subscribers.delete(subscriber);
                if (subscribers.size === 0 && stop) {
                    stop();
                    stop = null;
                }
            };
        }
        return { set, update, subscribe };
    }
    function derived(stores, fn, initial_value) {
        const single = !Array.isArray(stores);
        const stores_array = single
            ? [stores]
            : stores;
        const auto = fn.length < 2;
        return readable(initial_value, (set) => {
            let started = false;
            const values = [];
            let pending = 0;
            let cleanup = noop;
            const sync = () => {
                if (pending) {
                    return;
                }
                cleanup();
                const result = fn(single ? values[0] : values, set);
                if (auto) {
                    set(result);
                }
                else {
                    cleanup = is_function(result) ? result : noop;
                }
            };
            const unsubscribers = stores_array.map((store, i) => subscribe(store, (value) => {
                values[i] = value;
                pending &= ~(1 << i);
                if (started) {
                    sync();
                }
            }, () => {
                pending |= (1 << i);
            }));
            started = true;
            sync();
            return function stop() {
                run_all(unsubscribers);
                cleanup();
                // We need to set this to false because callbacks can still happen despite having unsubscribed:
                // Callbacks might already be placed in the queue which doesn't know it should no longer
                // invoke this derived store.
                started = false;
            };
        });
    }

    const ENV = {
      prod: "production",
      dev: "development",
      test: "testing",
    };

    function getCurrentEnv() {
      return ENV.test
    }

    function isDev() {
      return getCurrentEnv() === ENV.dev
    }

    /*
     * State handling for persistent data, websites/tabs data and data of the website of the active tab
     */

    /*
     * Persistent data
     * Custom svelte store for setting and listening to changes in local storage.
     */
    function createSettings() {
      const settings = {};

      const { subscribe, set, update } = writable(settings);

      // Get persistent data
      chrome.storage.sync.get(null, (stored_settings) => {
        set(stored_settings);

        // IMPORTANT: Get and set only specific values from local storage. You should never add private data here.
        chrome.storage.local.get(
          [
            "isLoggedIn",
            "isProUser",
            "translatorServiceType",
            "writeServiceType",
            "apiServiceType",
            "docTranslatorServiceType",
            "browserInstanceId",
            "featureSet",
          ],
          (local_settings) => {
            update((settings) => ({ ...settings, ...local_settings }));
          }
        );
      });

      // Listen to changes in storage and update store
      chrome.storage.onChanged.addListener((changes) => {
        // IMPORTANT List here all privacy-related keys that must not be used in UI elements!
        const NOT_PERMITTED_KEYS = ["a_t", "r_t", "i_t", "accountId"];

        let newSettings = {};
        for (const [key, value] of Object.entries(changes)) {
          if (NOT_PERMITTED_KEYS.includes(key) === false) {
            newSettings[key] = value.newValue;
          }
        }

        if (isDev()) {
          const style =
            "background-color: #324054;padding: 3px; font-size:12px;color:#e1e8c3; font-weight:600";
          console.log(
            `%c${new Date().toISOString()} UPDATE USER SETTINGS`,
            style,
            newSettings
          );
        }

        update((settings) => ({ ...settings, ...newSettings }));
      });

      // Provide only the set and subscribe method to ensure that all changes in storage are handled in this function
      return {
        subscribe,
        set: (setting) => chrome.storage.sync.set(setting),
      }
    }

    /*
     * Website data
     * Custom svelte store for setting and listening on changes of all open websites (all tabs).
     */
    function createWebsiteSettings() {
      let webSiteSettings = {};

      const { subscribe, update } = writable(webSiteSettings);

      // Get data of the website of the currently active tab and set corresponding state.
      const sendGetWebsiteSettings = () => {
        if (!chrome.tabs) {
          return
        }

        chrome.tabs.query({ active: true, currentWindow: true }, function (tabs) {
          chrome.tabs.sendMessage(
            tabs[0].id,
            { action: "dlGetWebsiteData" },
            function (response) {
              update((settings) => ({
                ...settings,
                [tabs[0].id]: { ...settings[tabs[0].id], ...response },
              }));
            }
          );
        });
      };

      // Listen to changes and update state of current active tab
      chrome.runtime.onMessage.addListener(function (request, sender) {
        switch (request.action) {
          case "newWebsiteState":
            if (request.payload) {
              update((settings) => {
                return {
                  ...settings,
                  [sender.tab.id]: {
                    ...settings[sender.tab.id],
                    ...request.payload,
                    error: request.payload.error
                      ? request.payload.error
                      : undefined,
                  },
                }
              });
            }

            break
          case "resetWebsiteState":
            if (request.payload) {
              update((settings) => ({
                ...settings,
                [sender.tab.id]: { ...request.payload },
              }));
            }
            break
        }
      });

      return {
        subscribe,
        fetchCurrentWebsiteSettings: sendGetWebsiteSettings,
      }
    }

    // Persistent Data
    const settings = createSettings();

    //Website Data
    const allTabsSettings = createWebsiteSettings();

    /*
     * Derived state of all web pages/tab data and creation of a new custom state of the active tab's web page.
     * see here for more information about dervied stores: https://svelte.dev/tutorial/derived-stores
     */
    const websiteSettings = derived(
      allTabsSettings,
      async ($settings, set) => {
        if (!chrome.tabs) {
          return
        }

        let queryOptions = { active: true, currentWindow: true };
        const tabs = await chrome.tabs.query(queryOptions);
        set($settings[tabs[0].id]);
      }
    );

    /**
     * Creates a state management object for experiments, initializing with an empty array.
     *
     * This function constructs a writable store (from Svelte's store module) for experiments.
     * It then sends an action to fetch the current set of experiments and updates the store
     * accordingly. Moreover, it sets up a listener for updates from the Chrome runtime,
     * allowing for synchronization of the experiments data across different parts of the extension.
     *
     * @returns {Object} An object with a `subscribe` method to allow components to reactively access the experiments state.
     */
    function createExperimentState() {
      const experimentState = [];

      const { subscribe, set } = writable(experimentState);

      sendAction({
        action: "dlGetExperiments",
        payload: {
          forceFetch: false,
        },
      }).then((exp) => set(exp));

      // Set up a listener for Chrome runtime messages.
      // When a message indicating that the experiments have been updated is received,
      // the store is updated with the new data.
      chrome.runtime.onMessage.addListener(function (request) {
        switch (request.action) {
          case "dlExperimentsUpdated":
            if (request.payload) {
              set(request.payload.experiments);
            }
            break
        }
      });

      return {
        subscribe,
      }
    }

    const experimentState = createExperimentState();

    /* src/devPage/DevMain.svelte generated by Svelte v3.59.2 */

    const { console: console_1 } = globals;
    const file = "src/devPage/DevMain.svelte";

    // (103:4) <AccordionSection headline="enviroment">
    function create_default_slot_8(ctx) {
    	let jsoneditor;
    	let current;

    	jsoneditor = new svelteJsoneditor.JSONEditor({
    			props: {
    				content: {
    					json: {
    						"IS_PROD": false,
    						"BASE_URLS": {
    							"dapApi": "https://s.deepl.dev/chrome/statistics",
    							"toolingApiPro": "https://api-test.deepl.com/jsonrpc?client=chrome-extension,1.18.0",
    							"toolingApi": "https://test.deepl.com/jsonrpc?client=chrome-extension,1.18.0",
    							"toolingWriteApiPro": "https://write-pro.www.test.deepl.com/jsonrpc?client=chrome-extension,1.18.0",
    							"toolingWriteApi": "https://write-free.www.test.deepl.com/jsonrpc?client=chrome-extension,1.18.0",
    							"classicApi": "https://test.deepl.com/jsonrpc?client=chrome-extension,1.18.0",
    							"classicApiPro": "https://api-test.deepl.com/jsonrpc?client=chrome-extension,1.18.0",
    							"classicApiWrite": "https://write-free.www.test.deepl.com/jsonrpc?client=chrome-extension,1.18.0",
    							"classicApiWritePro": "https://write-pro.www.test.deepl.com/jsonrpc?client=chrome-extension,1.18.0",
    							"classicApiGlossary": "https://api-test.deepl.com/termbases/jsonrpc?client=chrome-extension,1.18.0",
    							"login": "https://main.test.deepl.com/auth/login",
    							"token": "https://backend-test.deepl.com/oidc/token",
    							"logout": "https://main.test.deepl.com/auth/logout",
    							"feedbackSurvey": "https://deepl.typeform.com/to/MkMfP2Y6",
    							"newFeedbackSurvey": "https://deepl.qualtrics.com/jfe/form/SV_cMvbeX0WZGvkKGi",
    							"firefoxFeedbackSurvey": "https://deepl.qualtrics.com/jfe/form/SV_eyfDZqQmXuNTFOK",
    							"uninstallSurvey": "https://deepl.qualtrics.com/jfe/form/SV_eJxAD8c7J5hqSRU",
    							"gslidesFeedbackSurvey": "https://deepl.qualtrics.com/jfe/form/SV_9XBLSAbDmLq5emO",
    							"deeplPro": "https://main.test.deepl.com/pro",
    							"deeplProWrite": "https://main.test.deepl.com/pro-write",
    							"deeplTranslator": "https://main.test.deepl.com/translator",
    							"deeplWrite": "https://main.test.deepl.com/write",
    							"deeplDoctrans": "https://main.test.deepl.com/translator/files",
    							"deeplTermsOfServiceFree": "https://www.deepl.com/en/pro-license?tab=free",
    							"deeplCreateAccount": "https://main.test.deepl.com/signup",
    							"extensionStorePage": "https://bit.ly/DeepL-Chrome",
    							"supportArticle": "https://support.deepl.com/hc/articles/4407580229522",
    							"deeplSupport": "https://support.deepl.com/hc",
    							"chromeWebstore": "https://chrome.google.com/webstore/detail/deepl-translate-reading-w/cofdbpoegempjloogbagkncekinflcnj",
    							"edgeWebstore": "https://microsoftedge.microsoft.com/addons/detail/deepl-translate-reading-/fancfknaplihpclbhbpclnmmjcjanbaf",
    							"firefoxWebstore": "https://addons.mozilla.org/en-US/firefox/addon/deepl-translate",
    							"firefoxShortcutDocs": "https://support.mozilla.org/kb/manage-extension-shortcuts-firefox",
    							"clientStateApi": "https://backend-test.deepl.com/web",
    							"experimentsEndpoint": "https://s.deepl.dev/chrome/experiments",
    							"gmailFeedbackSurvey": "https://deepl.qualtrics.com/jfe/form/SV_db8ItFTmYBcZjYW",
    							"customerSegmentationSurveyFree": "https://deepl.qualtrics.com/jfe/form/SV_3LbBcdg4mjQuCCq?desc=TG-2071",
    							"customerSegmentationSurveyPro": "https://deepl.qualtrics.com/jfe/form/SV_3LbBcdg4mjQuCCq?desc=TG-2070",
    							"arabicSurvey": "https://deepl.qualtrics.com/jfe/form/SV_ekTrC2ePp0wn7xQ"
    						},
    						"APP_VERSION": "1.18.0",
    						"FEATURE_FLAGS": {
    							"TRANSLATE_INPUT": true,
    							"PAGE_TRANSLATION": true,
    							"IMPROVE_WRITING": true,
    							"PRO_LOGIN": true,
    							"DOCUMENT_TRANSLATION": true,
    							"RECOMMEND_TO_A_FRIEND": true,
    							"WEBPAGE_CUSTOMIZER": true,
    							"GDOCS_INTEGRATION": true,
    							"GMAIL_INTEGRATION": true,
    							"ZENDESK_INTEGRATION": true,
    							"GSLIDES_INTEGRATION": true,
    							"ON_INSTALL_ONBOARDING": true,
    							"WHATSAPP_TRANSLATE_ALL_CHAT": false,
    							"DEEPL_WRITE_GENERAL": false,
    							"DEEPL_WRITE_GDOCS": true,
    							"SHOW_CUSTOMER_SEGMENTATION_SURVEY": false,
    							"ARABIC_SURVEY": true
    						},
    						"HOT_RELOAD": {
    							"webSocketServerPort": 9012,
    							"active": true
    						},
    						"IS_TEST": true
    					}
    				},
    				readOnly: true
    			},
    			$$inline: true
    		});

    	const block = {
    		c: function create() {
    			create_component(jsoneditor.$$.fragment);
    		},
    		m: function mount(target, anchor) {
    			mount_component(jsoneditor, target, anchor);
    			current = true;
    		},
    		p: noop,
    		i: function intro(local) {
    			if (current) return;
    			transition_in(jsoneditor.$$.fragment, local);
    			current = true;
    		},
    		o: function outro(local) {
    			transition_out(jsoneditor.$$.fragment, local);
    			current = false;
    		},
    		d: function destroy(detaching) {
    			destroy_component(jsoneditor, detaching);
    		}
    	};

    	dispatch_dev("SvelteRegisterBlock", {
    		block,
    		id: create_default_slot_8.name,
    		type: "slot",
    		source: "(103:4) <AccordionSection headline=\\\"enviroment\\\">",
    		ctx
    	});

    	return block;
    }

    // (106:4) <AccordionSection headline="storage">
    function create_default_slot_7(ctx) {
    	let jsoneditor;
    	let current;

    	jsoneditor = new svelteJsoneditor.JSONEditor({
    			props: {
    				content: { json: /*$settings*/ ctx[3] },
    				onChange: /*handleCustomSettingsChange*/ ctx[7]
    			},
    			$$inline: true
    		});

    	const block = {
    		c: function create() {
    			create_component(jsoneditor.$$.fragment);
    		},
    		m: function mount(target, anchor) {
    			mount_component(jsoneditor, target, anchor);
    			current = true;
    		},
    		p: function update(ctx, dirty) {
    			const jsoneditor_changes = {};
    			if (dirty & /*$settings*/ 8) jsoneditor_changes.content = { json: /*$settings*/ ctx[3] };
    			jsoneditor.$set(jsoneditor_changes);
    		},
    		i: function intro(local) {
    			if (current) return;
    			transition_in(jsoneditor.$$.fragment, local);
    			current = true;
    		},
    		o: function outro(local) {
    			transition_out(jsoneditor.$$.fragment, local);
    			current = false;
    		},
    		d: function destroy(detaching) {
    			destroy_component(jsoneditor, detaching);
    		}
    	};

    	dispatch_dev("SvelteRegisterBlock", {
    		block,
    		id: create_default_slot_7.name,
    		type: "slot",
    		source: "(106:4) <AccordionSection headline=\\\"storage\\\">",
    		ctx
    	});

    	return block;
    }

    // (112:4) <AccordionSection headline="active website settings">
    function create_default_slot_6(ctx) {
    	let jsoneditor;
    	let current;

    	jsoneditor = new svelteJsoneditor.JSONEditor({
    			props: {
    				content: { json: /*$websiteSettings*/ ctx[1] },
    				readOnly: true
    			},
    			$$inline: true
    		});

    	const block = {
    		c: function create() {
    			create_component(jsoneditor.$$.fragment);
    		},
    		m: function mount(target, anchor) {
    			mount_component(jsoneditor, target, anchor);
    			current = true;
    		},
    		p: function update(ctx, dirty) {
    			const jsoneditor_changes = {};
    			if (dirty & /*$websiteSettings*/ 2) jsoneditor_changes.content = { json: /*$websiteSettings*/ ctx[1] };
    			jsoneditor.$set(jsoneditor_changes);
    		},
    		i: function intro(local) {
    			if (current) return;
    			transition_in(jsoneditor.$$.fragment, local);
    			current = true;
    		},
    		o: function outro(local) {
    			transition_out(jsoneditor.$$.fragment, local);
    			current = false;
    		},
    		d: function destroy(detaching) {
    			destroy_component(jsoneditor, detaching);
    		}
    	};

    	dispatch_dev("SvelteRegisterBlock", {
    		block,
    		id: create_default_slot_6.name,
    		type: "slot",
    		source: "(112:4) <AccordionSection headline=\\\"active website settings\\\">",
    		ctx
    	});

    	return block;
    }

    // (115:4) <AccordionSection headline="all websites (tab ids)">
    function create_default_slot_5(ctx) {
    	let jsoneditor;
    	let current;

    	jsoneditor = new svelteJsoneditor.JSONEditor({
    			props: {
    				content: { json: /*$allTabsSettings*/ ctx[4] },
    				readOnly: true
    			},
    			$$inline: true
    		});

    	const block = {
    		c: function create() {
    			create_component(jsoneditor.$$.fragment);
    		},
    		m: function mount(target, anchor) {
    			mount_component(jsoneditor, target, anchor);
    			current = true;
    		},
    		p: function update(ctx, dirty) {
    			const jsoneditor_changes = {};
    			if (dirty & /*$allTabsSettings*/ 16) jsoneditor_changes.content = { json: /*$allTabsSettings*/ ctx[4] };
    			jsoneditor.$set(jsoneditor_changes);
    		},
    		i: function intro(local) {
    			if (current) return;
    			transition_in(jsoneditor.$$.fragment, local);
    			current = true;
    		},
    		o: function outro(local) {
    			transition_out(jsoneditor.$$.fragment, local);
    			current = false;
    		},
    		d: function destroy(detaching) {
    			destroy_component(jsoneditor, detaching);
    		}
    	};

    	dispatch_dev("SvelteRegisterBlock", {
    		block,
    		id: create_default_slot_5.name,
    		type: "slot",
    		source: "(115:4) <AccordionSection headline=\\\"all websites (tab ids)\\\">",
    		ctx
    	});

    	return block;
    }

    // (118:4) <AccordionSection headline="change log website settings">
    function create_default_slot_4(ctx) {
    	let jsoneditor;
    	let current;

    	jsoneditor = new svelteJsoneditor.JSONEditor({
    			props: {
    				content: { json: /*changeStack*/ ctx[0] },
    				readOnly: true
    			},
    			$$inline: true
    		});

    	const block = {
    		c: function create() {
    			create_component(jsoneditor.$$.fragment);
    		},
    		m: function mount(target, anchor) {
    			mount_component(jsoneditor, target, anchor);
    			current = true;
    		},
    		p: function update(ctx, dirty) {
    			const jsoneditor_changes = {};
    			if (dirty & /*changeStack*/ 1) jsoneditor_changes.content = { json: /*changeStack*/ ctx[0] };
    			jsoneditor.$set(jsoneditor_changes);
    		},
    		i: function intro(local) {
    			if (current) return;
    			transition_in(jsoneditor.$$.fragment, local);
    			current = true;
    		},
    		o: function outro(local) {
    			transition_out(jsoneditor.$$.fragment, local);
    			current = false;
    		},
    		d: function destroy(detaching) {
    			destroy_component(jsoneditor, detaching);
    		}
    	};

    	dispatch_dev("SvelteRegisterBlock", {
    		block,
    		id: create_default_slot_4.name,
    		type: "slot",
    		source: "(118:4) <AccordionSection headline=\\\"change log website settings\\\">",
    		ctx
    	});

    	return block;
    }

    // (121:4) <AccordionSection headline="client state (feature set)">
    function create_default_slot_3(ctx) {
    	let jsoneditor;
    	let current;

    	jsoneditor = new svelteJsoneditor.JSONEditor({
    			props: {
    				content: { json: /*clientState*/ ctx[2] },
    				readOnly: true
    			},
    			$$inline: true
    		});

    	const block = {
    		c: function create() {
    			create_component(jsoneditor.$$.fragment);
    		},
    		m: function mount(target, anchor) {
    			mount_component(jsoneditor, target, anchor);
    			current = true;
    		},
    		p: function update(ctx, dirty) {
    			const jsoneditor_changes = {};
    			if (dirty & /*clientState*/ 4) jsoneditor_changes.content = { json: /*clientState*/ ctx[2] };
    			jsoneditor.$set(jsoneditor_changes);
    		},
    		i: function intro(local) {
    			if (current) return;
    			transition_in(jsoneditor.$$.fragment, local);
    			current = true;
    		},
    		o: function outro(local) {
    			transition_out(jsoneditor.$$.fragment, local);
    			current = false;
    		},
    		d: function destroy(detaching) {
    			destroy_component(jsoneditor, detaching);
    		}
    	};

    	dispatch_dev("SvelteRegisterBlock", {
    		block,
    		id: create_default_slot_3.name,
    		type: "slot",
    		source: "(121:4) <AccordionSection headline=\\\"client state (feature set)\\\">",
    		ctx
    	});

    	return block;
    }

    // (124:4) <AccordionSection headline="experiments">
    function create_default_slot_2(ctx) {
    	let jsoneditor;
    	let current;

    	jsoneditor = new svelteJsoneditor.JSONEditor({
    			props: {
    				content: { json: /*$experimentState*/ ctx[5] },
    				readOnly: true
    			},
    			$$inline: true
    		});

    	const block = {
    		c: function create() {
    			create_component(jsoneditor.$$.fragment);
    		},
    		m: function mount(target, anchor) {
    			mount_component(jsoneditor, target, anchor);
    			current = true;
    		},
    		p: function update(ctx, dirty) {
    			const jsoneditor_changes = {};
    			if (dirty & /*$experimentState*/ 32) jsoneditor_changes.content = { json: /*$experimentState*/ ctx[5] };
    			jsoneditor.$set(jsoneditor_changes);
    		},
    		i: function intro(local) {
    			if (current) return;
    			transition_in(jsoneditor.$$.fragment, local);
    			current = true;
    		},
    		o: function outro(local) {
    			transition_out(jsoneditor.$$.fragment, local);
    			current = false;
    		},
    		d: function destroy(detaching) {
    			destroy_component(jsoneditor, detaching);
    		}
    	};

    	dispatch_dev("SvelteRegisterBlock", {
    		block,
    		id: create_default_slot_2.name,
    		type: "slot",
    		source: "(124:4) <AccordionSection headline=\\\"experiments\\\">",
    		ctx
    	});

    	return block;
    }

    // (127:4) <AccordionSection headline="classic api test">
    function create_default_slot_1(ctx) {
    	let classicapitest;
    	let current;
    	classicapitest = new ClassicApiTest({ $$inline: true });

    	const block = {
    		c: function create() {
    			create_component(classicapitest.$$.fragment);
    		},
    		m: function mount(target, anchor) {
    			mount_component(classicapitest, target, anchor);
    			current = true;
    		},
    		i: function intro(local) {
    			if (current) return;
    			transition_in(classicapitest.$$.fragment, local);
    			current = true;
    		},
    		o: function outro(local) {
    			transition_out(classicapitest.$$.fragment, local);
    			current = false;
    		},
    		d: function destroy(detaching) {
    			destroy_component(classicapitest, detaching);
    		}
    	};

    	dispatch_dev("SvelteRegisterBlock", {
    		block,
    		id: create_default_slot_1.name,
    		type: "slot",
    		source: "(127:4) <AccordionSection headline=\\\"classic api test\\\">",
    		ctx
    	});

    	return block;
    }

    // (102:2) <Accordion>
    function create_default_slot(ctx) {
    	let accordionsection0;
    	let t0;
    	let accordionsection1;
    	let t1;
    	let accordionsection2;
    	let t2;
    	let accordionsection3;
    	let t3;
    	let accordionsection4;
    	let t4;
    	let accordionsection5;
    	let t5;
    	let accordionsection6;
    	let t6;
    	let accordionsection7;
    	let current;

    	accordionsection0 = new AccordionSection({
    			props: {
    				headline: "enviroment",
    				$$slots: { default: [create_default_slot_8] },
    				$$scope: { ctx }
    			},
    			$$inline: true
    		});

    	accordionsection1 = new AccordionSection({
    			props: {
    				headline: "storage",
    				$$slots: { default: [create_default_slot_7] },
    				$$scope: { ctx }
    			},
    			$$inline: true
    		});

    	accordionsection2 = new AccordionSection({
    			props: {
    				headline: "active website settings",
    				$$slots: { default: [create_default_slot_6] },
    				$$scope: { ctx }
    			},
    			$$inline: true
    		});

    	accordionsection3 = new AccordionSection({
    			props: {
    				headline: "all websites (tab ids)",
    				$$slots: { default: [create_default_slot_5] },
    				$$scope: { ctx }
    			},
    			$$inline: true
    		});

    	accordionsection4 = new AccordionSection({
    			props: {
    				headline: "change log website settings",
    				$$slots: { default: [create_default_slot_4] },
    				$$scope: { ctx }
    			},
    			$$inline: true
    		});

    	accordionsection5 = new AccordionSection({
    			props: {
    				headline: "client state (feature set)",
    				$$slots: { default: [create_default_slot_3] },
    				$$scope: { ctx }
    			},
    			$$inline: true
    		});

    	accordionsection6 = new AccordionSection({
    			props: {
    				headline: "experiments",
    				$$slots: { default: [create_default_slot_2] },
    				$$scope: { ctx }
    			},
    			$$inline: true
    		});

    	accordionsection7 = new AccordionSection({
    			props: {
    				headline: "classic api test",
    				$$slots: { default: [create_default_slot_1] },
    				$$scope: { ctx }
    			},
    			$$inline: true
    		});

    	const block = {
    		c: function create() {
    			create_component(accordionsection0.$$.fragment);
    			t0 = space();
    			create_component(accordionsection1.$$.fragment);
    			t1 = space();
    			create_component(accordionsection2.$$.fragment);
    			t2 = space();
    			create_component(accordionsection3.$$.fragment);
    			t3 = space();
    			create_component(accordionsection4.$$.fragment);
    			t4 = space();
    			create_component(accordionsection5.$$.fragment);
    			t5 = space();
    			create_component(accordionsection6.$$.fragment);
    			t6 = space();
    			create_component(accordionsection7.$$.fragment);
    		},
    		m: function mount(target, anchor) {
    			mount_component(accordionsection0, target, anchor);
    			insert_dev(target, t0, anchor);
    			mount_component(accordionsection1, target, anchor);
    			insert_dev(target, t1, anchor);
    			mount_component(accordionsection2, target, anchor);
    			insert_dev(target, t2, anchor);
    			mount_component(accordionsection3, target, anchor);
    			insert_dev(target, t3, anchor);
    			mount_component(accordionsection4, target, anchor);
    			insert_dev(target, t4, anchor);
    			mount_component(accordionsection5, target, anchor);
    			insert_dev(target, t5, anchor);
    			mount_component(accordionsection6, target, anchor);
    			insert_dev(target, t6, anchor);
    			mount_component(accordionsection7, target, anchor);
    			current = true;
    		},
    		p: function update(ctx, dirty) {
    			const accordionsection0_changes = {};

    			if (dirty & /*$$scope*/ 32768) {
    				accordionsection0_changes.$$scope = { dirty, ctx };
    			}

    			accordionsection0.$set(accordionsection0_changes);
    			const accordionsection1_changes = {};

    			if (dirty & /*$$scope, $settings*/ 32776) {
    				accordionsection1_changes.$$scope = { dirty, ctx };
    			}

    			accordionsection1.$set(accordionsection1_changes);
    			const accordionsection2_changes = {};

    			if (dirty & /*$$scope, $websiteSettings*/ 32770) {
    				accordionsection2_changes.$$scope = { dirty, ctx };
    			}

    			accordionsection2.$set(accordionsection2_changes);
    			const accordionsection3_changes = {};

    			if (dirty & /*$$scope, $allTabsSettings*/ 32784) {
    				accordionsection3_changes.$$scope = { dirty, ctx };
    			}

    			accordionsection3.$set(accordionsection3_changes);
    			const accordionsection4_changes = {};

    			if (dirty & /*$$scope, changeStack*/ 32769) {
    				accordionsection4_changes.$$scope = { dirty, ctx };
    			}

    			accordionsection4.$set(accordionsection4_changes);
    			const accordionsection5_changes = {};

    			if (dirty & /*$$scope, clientState*/ 32772) {
    				accordionsection5_changes.$$scope = { dirty, ctx };
    			}

    			accordionsection5.$set(accordionsection5_changes);
    			const accordionsection6_changes = {};

    			if (dirty & /*$$scope, $experimentState*/ 32800) {
    				accordionsection6_changes.$$scope = { dirty, ctx };
    			}

    			accordionsection6.$set(accordionsection6_changes);
    			const accordionsection7_changes = {};

    			if (dirty & /*$$scope*/ 32768) {
    				accordionsection7_changes.$$scope = { dirty, ctx };
    			}

    			accordionsection7.$set(accordionsection7_changes);
    		},
    		i: function intro(local) {
    			if (current) return;
    			transition_in(accordionsection0.$$.fragment, local);
    			transition_in(accordionsection1.$$.fragment, local);
    			transition_in(accordionsection2.$$.fragment, local);
    			transition_in(accordionsection3.$$.fragment, local);
    			transition_in(accordionsection4.$$.fragment, local);
    			transition_in(accordionsection5.$$.fragment, local);
    			transition_in(accordionsection6.$$.fragment, local);
    			transition_in(accordionsection7.$$.fragment, local);
    			current = true;
    		},
    		o: function outro(local) {
    			transition_out(accordionsection0.$$.fragment, local);
    			transition_out(accordionsection1.$$.fragment, local);
    			transition_out(accordionsection2.$$.fragment, local);
    			transition_out(accordionsection3.$$.fragment, local);
    			transition_out(accordionsection4.$$.fragment, local);
    			transition_out(accordionsection5.$$.fragment, local);
    			transition_out(accordionsection6.$$.fragment, local);
    			transition_out(accordionsection7.$$.fragment, local);
    			current = false;
    		},
    		d: function destroy(detaching) {
    			destroy_component(accordionsection0, detaching);
    			if (detaching) detach_dev(t0);
    			destroy_component(accordionsection1, detaching);
    			if (detaching) detach_dev(t1);
    			destroy_component(accordionsection2, detaching);
    			if (detaching) detach_dev(t2);
    			destroy_component(accordionsection3, detaching);
    			if (detaching) detach_dev(t3);
    			destroy_component(accordionsection4, detaching);
    			if (detaching) detach_dev(t4);
    			destroy_component(accordionsection5, detaching);
    			if (detaching) detach_dev(t5);
    			destroy_component(accordionsection6, detaching);
    			if (detaching) detach_dev(t6);
    			destroy_component(accordionsection7, detaching);
    		}
    	};

    	dispatch_dev("SvelteRegisterBlock", {
    		block,
    		id: create_default_slot.name,
    		type: "slot",
    		source: "(102:2) <Accordion>",
    		ctx
    	});

    	return block;
    }

    function create_fragment(ctx) {
    	let div1;
    	let div0;
    	let button0;
    	let t1;
    	let button1;
    	let t3;
    	let button2;
    	let t5;
    	let button3;
    	let t7;
    	let label;
    	let input;
    	let input_checked_value;
    	let t8;
    	let t9;
    	let accordion;
    	let current;
    	let mounted;
    	let dispose;

    	accordion = new Accordion({
    			props: {
    				$$slots: { default: [create_default_slot] },
    				$$scope: { ctx }
    			},
    			$$inline: true
    		});

    	const block = {
    		c: function create() {
    			div1 = element("div");
    			div0 = element("div");
    			button0 = element("button");
    			button0.textContent = "get experiments";
    			t1 = space();
    			button1 = element("button");
    			button1.textContent = "force fetch experiments";
    			t3 = space();
    			button2 = element("button");
    			button2.textContent = "get client state";
    			t5 = space();
    			button3 = element("button");
    			button3.textContent = "force fetch client state";
    			t7 = space();
    			label = element("label");
    			input = element("input");
    			t8 = text("\n      show experiments");
    			t9 = space();
    			create_component(accordion.$$.fragment);
    			add_location(button0, file, 87, 4, 1910);
    			add_location(button1, file, 88, 4, 1986);
    			add_location(button2, file, 91, 4, 2081);
    			add_location(button3, file, 92, 4, 2158);
    			attr_dev(input, "type", "checkbox");
    			input.checked = input_checked_value = /*$settings*/ ctx[3].showExperiments;
    			add_location(input, file, 97, 6, 2356);
    			add_location(label, file, 96, 4, 2315);
    			attr_dev(div0, "class", "button-container svelte-c4o4kg");
    			add_location(div0, file, 86, 2, 1875);
    			attr_dev(div1, "class", "settings-container svelte-c4o4kg");
    			attr_dev(div1, "dir", getTextDirection());
    			add_location(div1, file, 85, 0, 1815);
    		},
    		l: function claim(nodes) {
    			throw new Error("options.hydrate only works if the component was compiled with the `hydratable: true` option");
    		},
    		m: function mount(target, anchor) {
    			insert_dev(target, div1, anchor);
    			append_dev(div1, div0);
    			append_dev(div0, button0);
    			append_dev(div0, t1);
    			append_dev(div0, button1);
    			append_dev(div0, t3);
    			append_dev(div0, button2);
    			append_dev(div0, t5);
    			append_dev(div0, button3);
    			append_dev(div0, t7);
    			append_dev(div0, label);
    			append_dev(label, input);
    			append_dev(label, t8);
    			append_dev(div1, t9);
    			mount_component(accordion, div1, null);
    			current = true;

    			if (!mounted) {
    				dispose = [
    					listen_dev(button0, "click", /*click_handler*/ ctx[10], false, false, false, false),
    					listen_dev(button1, "click", /*click_handler_1*/ ctx[11], false, false, false, false),
    					listen_dev(button2, "click", /*click_handler_2*/ ctx[12], false, false, false, false),
    					listen_dev(button3, "click", /*click_handler_3*/ ctx[13], false, false, false, false),
    					listen_dev(label, "click", /*showExperiments*/ ctx[6], false, false, false, false)
    				];

    				mounted = true;
    			}
    		},
    		p: function update(ctx, [dirty]) {
    			if (!current || dirty & /*$settings*/ 8 && input_checked_value !== (input_checked_value = /*$settings*/ ctx[3].showExperiments)) {
    				prop_dev(input, "checked", input_checked_value);
    			}

    			const accordion_changes = {};

    			if (dirty & /*$$scope, $experimentState, clientState, changeStack, $allTabsSettings, $websiteSettings, $settings*/ 32831) {
    				accordion_changes.$$scope = { dirty, ctx };
    			}

    			accordion.$set(accordion_changes);
    		},
    		i: function intro(local) {
    			if (current) return;
    			transition_in(accordion.$$.fragment, local);
    			current = true;
    		},
    		o: function outro(local) {
    			transition_out(accordion.$$.fragment, local);
    			current = false;
    		},
    		d: function destroy(detaching) {
    			if (detaching) detach_dev(div1);
    			destroy_component(accordion);
    			mounted = false;
    			run_all(dispose);
    		}
    	};

    	dispatch_dev("SvelteRegisterBlock", {
    		block,
    		id: create_fragment.name,
    		type: "component",
    		source: "",
    		ctx
    	});

    	return block;
    }

    function instance($$self, $$props, $$invalidate) {
    	let $settings;
    	let $websiteSettings;
    	let $allTabsSettings;
    	let $experimentState;
    	validate_store(settings, 'settings');
    	component_subscribe($$self, settings, $$value => $$invalidate(3, $settings = $$value));
    	validate_store(websiteSettings, 'websiteSettings');
    	component_subscribe($$self, websiteSettings, $$value => $$invalidate(1, $websiteSettings = $$value));
    	validate_store(allTabsSettings, 'allTabsSettings');
    	component_subscribe($$self, allTabsSettings, $$value => $$invalidate(4, $allTabsSettings = $$value));
    	validate_store(experimentState, 'experimentState');
    	component_subscribe($$self, experimentState, $$value => $$invalidate(5, $experimentState = $$value));
    	let { $$slots: slots = {}, $$scope } = $$props;
    	validate_slots('DevMain', slots, []);
    	setContext("text_direction", getTextDirection());
    	let changeStack = [];
    	let clientState;
    	let error;

    	function showExperiments() {
    		settings.set({
    			showExperiments: !$settings.showExperiments
    		});
    	}

    	function handleCustomSettingsChange(updatedContent, previousContent, patchResult) {
    		console.log("onCustomSettingsChange ", updatedContent, previousContent, patchResult);
    		settings.set(updatedContent.json);
    	}

    	async function getExperiments(forceFetch) {
    		try {
    			await sendAction({
    				action: "dlGetExperiments",
    				payload: { forceFetch }
    			});
    		} catch(err) {
    			error = err;
    		}
    	}

    	async function getClientState(forceFetch) {
    		try {
    			$$invalidate(2, clientState = await sendAction({
    				action: "dlGetClientState",
    				payload: { forceFetch }
    			}));
    		} catch(err) {
    			error = err;
    		}
    	}

    	onMount(() => {
    		sendAction({ action: "dlGetClientState" }).then(state => $$invalidate(2, clientState = state)).catch(err => {
    			error = err;
    		});
    	});

    	const writable_props = [];

    	Object.keys($$props).forEach(key => {
    		if (!~writable_props.indexOf(key) && key.slice(0, 2) !== '$$' && key !== 'slot') console_1.warn(`<DevMain> was created with unknown prop '${key}'`);
    	});

    	const click_handler = () => getExperiments(false);
    	const click_handler_1 = () => getExperiments(true);
    	const click_handler_2 = () => getClientState(false);
    	const click_handler_3 = () => getClientState(true);

    	$$self.$capture_state = () => ({
    		onMount,
    		setContext,
    		Accordion,
    		AccordionSection,
    		ClassicApiTest,
    		JSONEditor: svelteJsoneditor.JSONEditor,
    		getTextDirection,
    		settings,
    		websiteSettings,
    		allTabsSettings,
    		experimentState,
    		sendAction,
    		changeStack,
    		clientState,
    		error,
    		showExperiments,
    		handleCustomSettingsChange,
    		getExperiments,
    		getClientState,
    		$settings,
    		$websiteSettings,
    		$allTabsSettings,
    		$experimentState
    	});

    	$$self.$inject_state = $$props => {
    		if ('changeStack' in $$props) $$invalidate(0, changeStack = $$props.changeStack);
    		if ('clientState' in $$props) $$invalidate(2, clientState = $$props.clientState);
    		if ('error' in $$props) error = $$props.error;
    	};

    	if ($$props && "$$inject" in $$props) {
    		$$self.$inject_state($$props.$$inject);
    	}

    	$$self.$$.update = () => {
    		if ($$self.$$.dirty & /*changeStack, $websiteSettings*/ 3) {
    			{
    				$$invalidate(0, changeStack = [
    					...changeStack,
    					{
    						time: Date.now(),
    						state: { ...$websiteSettings }
    					}
    				]);
    			}
    		}
    	};

    	return [
    		changeStack,
    		$websiteSettings,
    		clientState,
    		$settings,
    		$allTabsSettings,
    		$experimentState,
    		showExperiments,
    		handleCustomSettingsChange,
    		getExperiments,
    		getClientState,
    		click_handler,
    		click_handler_1,
    		click_handler_2,
    		click_handler_3
    	];
    }

    class DevMain extends SvelteComponentDev {
    	constructor(options) {
    		super(options);
    		init(this, options, instance, create_fragment, safe_not_equal, {});

    		dispatch_dev("SvelteRegisterComponent", {
    			component: this,
    			tagName: "DevMain",
    			options,
    			id: create_fragment.name
    		});
    	}
    }

    new DevMain({
      target: document.body,
    });

})(svelteJsoneditor);
//# sourceMappingURL=dev-page.js.map

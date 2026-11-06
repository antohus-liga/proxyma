import enGB from './en-GB'

const ptPT: typeof enGB = {
  app: {
    name: 'Proxyma',
    wordmark: 'proxyma',
    homeLabel: 'Página inicial da Proxyma',
  },
  a11y: {
    skipToContent: 'Saltar para o conteúdo principal',
  },
  common: {
    required: 'obrigatório',
    close: 'Fechar',
    cancel: 'Cancelar',
  },
  nav: {
    account: 'Conta',
    mainSection: 'Principal',
    settings: 'Definições',
    signIn: 'Iniciar sessão',
    createAccount: 'Criar uma conta',
    signOut: 'Terminar sessão',
    openMenu: 'Abrir navegação',
    closeMenu: 'Fechar navegação',
    adminSection: 'Administração da plataforma',
    backToSettings: 'Voltar às definições',
    admin: {
      overview: 'Visão geral',
      users: 'Utilizadores',
      orgs: 'Organizações',
      audit: 'Auditoria',
    },
  },
  theme: {
    label: 'Tema',
    system: 'Usar o tema do sistema',
    light: 'Usar o tema claro',
    dark: 'Usar o tema escuro',
  },
  locale: {
    label: 'Idioma',
    english: 'English (UK)',
    portuguese: 'Português (Portugal)',
  },
  settings: {
    title: 'Definições',
    description: 'Gerir a sua conta, a aparência e as credenciais de início de sessão.',
    sections: {
      label: 'Secções das definições',
      account: 'Conta',
      general: 'Geral',
      security: 'Segurança',
    },
    account: {
      meta: {
        title: 'Definições da conta — Proxyma',
      },
    },
    general: {
      meta: {
        title: 'Definições gerais — Proxyma',
      },
      appearance: {
        title: 'Aparência',
        description: 'Escolha o aspeto da Proxyma neste dispositivo.',
      },
      language: {
        title: 'Idioma',
        description: 'A Proxyma segue o idioma do seu navegador até escolher um aqui.',
      },
    },
    security: {
      meta: {
        title: 'Definições de segurança — Proxyma',
      },
    },
  },
  account: {
    profile: {
      title: 'Perfil',
      description: 'O nome que a Proxyma mostra para si em todo o painel.',
      displayName: {
        label: 'Nome a apresentar',
        help: 'Utilize entre 1 e 80 caracteres.',
      },
      email: {
        label: 'Email',
        help: 'Contacte o operador da plataforma para alterar o email da sua conta.',
      },
      type: {
        label: 'Tipo de conta',
        admin: 'Administrador da plataforma',
        standard: 'Conta padrão',
      },
      save: 'Guardar alterações',
      success: 'O seu perfil foi atualizado.',
      fieldErrors: {
        required: 'Introduza um nome a apresentar.',
        tooLong: 'Utilize 80 caracteres ou menos.',
        invalid: 'Introduza um nome a apresentar com 80 caracteres ou menos.',
      },
    },
    password: {
      meta: {
        title: 'Palavra-passe — Proxyma',
      },
      title: 'Palavra-passe',
      description: 'Altere a palavra-passe que utiliza para iniciar sessão na Proxyma.',
      warning: {
        title: 'A sua sessão será terminada',
        description:
          'Alterar a palavra-passe termina a sessão em todos os dispositivos, incluindo este. Inicie sessão novamente com a nova palavra-passe.',
      },
      currentPassword: {
        label: 'Palavra-passe atual',
      },
      newPassword: {
        label: 'Nova palavra-passe',
        help: 'Utilize pelo menos 10 caracteres. Evite palavras-passe comuns.',
      },
      confirmPassword: {
        label: 'Confirmar a nova palavra-passe',
      },
      submit: 'Alterar palavra-passe',
      fieldErrors: {
        currentRequired: 'Introduza a sua palavra-passe atual.',
        currentInvalid: 'Introduza a sua palavra-passe atual.',
        newRequired: 'Introduza uma nova palavra-passe.',
        newInvalid: 'Utilize uma nova palavra-passe entre 10 e 128 caracteres.',
        tooShort: 'Utilize pelo menos 10 caracteres.',
        tooLong: 'Utilize 128 caracteres ou menos.',
        confirmRequired: 'Confirme a nova palavra-passe.',
        mismatch: 'As palavras-passe não coincidem.',
      },
    },
    admin: {
      title: 'Administração da plataforma',
      description:
        'Consulte os utilizadores da plataforma, as organizações e o registo de auditoria administrativa.',
      open: 'Abrir a administração da plataforma',
    },
  },
  home: {
    meta: {
      title: 'Proxyma — Sempre perto, sempre presente.',
    },
    hero: {
      eyebrow: 'Proxy reverso gerido',
      title: 'Sempre perto, sempre presente.',
      lead: 'Um proxy reverso que não precisa de supervisão constante.',
      body: 'A Proxyma executa um contentor de proxy isolado para o seu projeto. Adicione rotas, defina limites de taxa e acompanhe o tráfego em direto a partir de um único painel.',
    },
    valueProps: {
      title: 'Porquê a Proxyma',
      proxy: {
        title: 'O seu próprio proxy',
        body: 'Um contentor e um URL isolados para cada organização.',
      },
      traffic: {
        title: 'Tráfego que pode ver',
        body: 'Pedidos em direto, latência e banimentos sem terminal.',
      },
      changes: {
        title: 'Alterações sem reiniciar',
        body: 'As rotas e os limites são aplicados como uma nova versão da configuração.',
      },
    },
    howItWorks: {
      title: 'Como funciona',
      intro: 'Três passos desde a criação da conta até ao primeiro pedido através do proxy.',
      steps: {
        account: {
          title: 'Crie uma conta',
          body: 'Comece com o seu email e uma palavra-passe.',
        },
        organisation: {
          title: 'Crie uma organização',
          body: 'A Proxyma inicia um contentor de proxy isolado e fornece o respetivo URL.',
        },
        traffic: {
          title: 'Encaminhe o tráfego para o seu proxy',
          body: 'Adicione uma rota, defina um limite de taxa e envie pedidos através dela.',
        },
      },
    },
    closing: {
      title: 'Sem configuração de servidor. Sem ficheiros de configuração editados à mão.',
      body: 'Crie uma conta e a Proxyma inicia um contentor de proxy isolado para o seu projeto.',
    },
  },
  footer: {
    copyright: '© {year} Proxyma',
  },
  auth: {
    signIn: {
      meta: {
        title: 'Iniciar sessão — Proxyma',
      },
      title: 'Iniciar sessão na Proxyma',
      description: 'Introduza o seu email e palavra-passe para gerir o seu proxy.',
      submit: 'Iniciar sessão',
      newHere: 'Ainda não tem conta na Proxyma?',
      createAccountLink: 'Criar uma conta',
      sessionExpired: 'A sua sessão terminou. Inicie sessão novamente.',
      passwordChanged: 'Palavra-passe alterada. Inicie sessão com a nova palavra-passe.',
    },
    createAccount: {
      meta: {
        title: 'Criar uma conta — Proxyma',
      },
      title: 'Crie a sua conta na Proxyma',
      description: 'Crie uma conta para começar a utilizar a Proxyma.',
      submit: 'Criar conta',
      haveAccount: 'Já tem uma conta?',
      signInLink: 'Iniciar sessão',
      passwordHelp: 'Utilize pelo menos 10 caracteres. Evite palavras-passe comuns.',
      emailTaken: 'Já existe uma conta com este email.',
      emailTakenLink: 'Iniciar sessão',
      success: {
        title: 'Conta criada',
        description: 'Inicie sessão para continuar.',
        signIn: 'Iniciar sessão',
      },
    },
    fields: {
      displayName: 'Nome a apresentar',
      email: 'Email',
      password: 'Palavra-passe',
    },
    password: {
      show: 'Mostrar',
      hide: 'Ocultar',
      showLabel: 'Mostrar palavra-passe',
      hideLabel: 'Ocultar palavra-passe',
    },
    fieldErrors: {
      displayName: {
        required: 'Introduza um nome a apresentar.',
        tooLong: 'Utilize 80 caracteres ou menos.',
        invalid: 'Introduza um nome a apresentar com 80 caracteres ou menos.',
      },
      email: {
        required: 'Introduza o seu endereço de email.',
        tooLong: 'Utilize 254 caracteres ou menos.',
        invalid: 'Introduza um endereço de email válido.',
      },
      password: {
        required: 'Introduza a sua palavra-passe.',
        tooShort: 'Utilize pelo menos 10 caracteres.',
        tooLong: 'Utilize 128 caracteres ou menos.',
        invalid: 'Utilize uma palavra-passe entre 10 e 128 caracteres.',
      },
    },
  },
  admin: {
    refresh: 'Atualizar',
    error: {
      title: 'Não foi possível carregar esta página',
      retry: 'Tentar novamente',
      loadMore: 'Não foi possível carregar mais resultados.',
    },
    filters: {
      apply: 'Aplicar filtros',
      clear: 'Limpar filtros',
    },
    list: {
      loadMore: 'Carregar mais',
      moreAvailable: 'Há mais resultados disponíveis.',
      allLoaded: 'Todos os resultados foram carregados.',
    },
    breadcrumbs: {
      label: 'Navegação estrutural',
    },
    status: {
      user: {
        active: 'Ativo',
        suspended: 'Suspenso',
      },
      org: {
        CREATING: 'A criar',
        STARTING: 'A iniciar',
        ONLINE: 'Online',
        OFFLINE: 'Offline',
        DELETING: 'A eliminar',
      },
      config: {
        applied: 'Aplicada',
        pending: 'Pendente',
        rejected: 'Rejeitada',
      },
    },
    roles: {
      owner: 'Proprietário',
      co_owner: 'Coproprietário',
      member: 'Membro',
    },
    overview: {
      meta: {
        title: 'Visão geral da plataforma — Proxyma',
      },
      title: 'Visão geral da plataforma',
      description:
        'Contagens de toda a plataforma e ligações rápidas a utilizadores, organizações e auditoria.',
      metrics: {
        users: 'Utilizadores',
        usersHint: 'Todas as contas registadas',
        suspended: 'Utilizadores suspensos',
        suspendedHint: 'Contas que não podem iniciar sessão',
        orgs: 'Organizações',
        orgsHint: 'Todas as organizações',
        online: 'Organizações online',
        onlineHint: 'Desligadas neste momento: {offline}',
      },
      links: {
        title: 'Administração',
        description:
          'Vistas apenas com metadados. O conteúdo das configurações, os registos e os endereços banidos permanecem privados.',
        users: 'Utilizadores',
        usersDescription: 'Pesquise contas e gerencie suspensões.',
        orgs: 'Organizações',
        orgsDescription: 'Inspecione o estado das organizações e a saúde do proxy.',
        audit: 'Auditoria',
        auditDescription: 'Reveja a atividade de administração da plataforma.',
      },
    },
    users: {
      meta: {
        title: 'Utilizadores — Proxyma',
      },
      title: 'Utilizadores',
      description: 'Pesquise contas, reveja as respetivas organizações e gerencie suspensões.',
      search: 'Pesquisar',
      searchPlaceholder: 'Email ou nome a apresentar',
      status: 'Estado',
      allStatuses: 'Todos os estados',
      tableCaption: 'Utilizadores da plataforma',
      columns: {
        user: 'Utilizador',
        status: 'Estado',
        orgs: 'Organizações',
        lastSignIn: 'Último início de sessão',
        created: 'Criado',
        actions: 'Ações',
      },
      orgSummary: 'Organizações: {count}',
      ownsOrg: 'É proprietário de uma organização',
      never: 'Nunca',
      view: 'Ver',
      loaded: 'Utilizadores carregados: {count}.',
      empty: {
        title: 'Nenhum utilizador corresponde a estes filtros',
        description: 'Experimente outro termo de pesquisa ou limpe o filtro de estado.',
      },
    },
    userDetail: {
      meta: {
        title: 'Detalhes do utilizador — Proxyma',
      },
      title: 'Detalhes do utilizador',
      description: 'Metadados da conta e associações a organizações.',
      back: 'Voltar aos utilizadores',
      account: {
        title: 'Conta',
        id: 'ID do utilizador',
        displayName: 'Nome a apresentar',
        email: 'Email',
        type: 'Tipo de conta',
        platformAdmin: 'Administrador da plataforma',
        standard: 'Conta padrão',
        created: 'Criado',
        lastSignIn: 'Último início de sessão',
        sessions: 'Sessões ativas',
        ownedOrg: 'Organização própria',
        noOrg: 'Sem organização própria',
      },
      suspension: {
        title: 'Suspensão',
        description: 'A conta está suspensa e não pode iniciar sessão.',
        reason: 'Motivo',
        noReason: 'Nenhum motivo registado',
        at: 'Suspensa em',
        by: 'Suspensa por',
        system: 'Operador',
      },
      memberships: {
        title: 'Associações a organizações',
        description: 'Todas as organizações a que esta conta pertence.',
        caption: 'Associações a organizações',
        org: 'Organização',
        role: 'Função',
        joined: 'Entrou em',
        empty: 'Esta conta não pertence a nenhuma organização.',
      },
      actions: {
        title: 'Ações da conta',
        description:
          'As ações de risco exigem a sua própria palavra-passe e ficam registadas na auditoria da plataforma.',
        accountHeading: 'Estado da conta',
        suspend: 'Suspender conta',
        unsuspend: 'Retirar suspensão',
        suspendTitle: 'Suspender {name}?',
        unsuspendTitle: 'Retirar a suspensão a {name}?',
        suspendDescription:
          'Isto revoga todas as sessões e bloqueia o início de sessão imediatamente. A organização e o respetivo proxy continuam a funcionar.',
        unsuspendDescription:
          'Isto restaura o início de sessão. As sessões revogadas durante a suspensão continuam terminadas.',
        suspendConfirm: 'Suspender conta',
        unsuspendConfirm: 'Retirar suspensão',
        suspendHint:
          'Suspender o utilizador termina a sessão e bloqueia o início de sessão. Não interrompe o proxy do utilizador.',
        unsuspendHint:
          'Esta conta está suspensa. Restaurar o acesso mantém as sessões antigas revogadas.',
        sessionsHeading: 'Sessões',
        forceLogout: 'Forçar terminação de sessão',
        forceLogoutTitle: 'Forçar {name} a terminar sessão?',
        forceLogoutDescription:
          'Isto revoga todas as sessões ativas ({count}). {name} pode iniciar sessão novamente de imediato.',
        forceLogoutConfirm: 'Forçar terminação de sessão',
        forceLogoutHint: 'Termina todas as sessões ativas sem suspender a conta.',
        noSessions: 'Esta conta não tem sessões ativas.',
      },
      success: {
        suspend: 'A conta foi suspensa.',
        unsuspend: 'A suspensão da conta foi retirada.',
        forceLogout: 'Todas as sessões desta conta foram revogadas.',
      },
    },
    orgs: {
      meta: {
        title: 'Organizações — Proxyma',
      },
      title: 'Organizações',
      description: 'Inspecione o estado das organizações, a propriedade e a saúde do proxy.',
      search: 'Pesquisar',
      searchPlaceholder: 'Organização, proprietário ou email',
      state: 'Estado',
      allStates: 'Todos os estados',
      tableCaption: 'Organizações da plataforma',
      columns: {
        org: 'Organização',
        state: 'Estado',
        owner: 'Proprietário',
        members: 'Membros',
        created: 'Criada',
        actions: 'Ações',
      },
      noOwner: 'Sem proprietário',
      view: 'Ver',
      loaded: 'Organizações carregadas: {count}.',
      empty: {
        title: 'Nenhuma organização corresponde a estes filtros',
        description: 'Experimente outro termo de pesquisa ou limpe o filtro de estado.',
      },
    },
    orgDetail: {
      meta: {
        title: 'Detalhes da organização — Proxyma',
      },
      title: 'Detalhes da organização',
      description: 'Metadados da organização e saúde do proxy.',
      back: 'Voltar às organizações',
      readOnly:
        'Esta página é só de leitura. As ações de conta para o proprietário estão disponíveis na página de utilizador do proprietário.',
      summary: {
        title: 'Organização',
        id: 'ID da organização',
        state: 'Estado',
        created: 'Criada',
        owner: 'Proprietário',
        ownerActions:
          'Para suspender o proprietário ou terminar a sessão dele, utilize a página de utilizador do proprietário.',
        openOwner: 'Abrir a conta do proprietário',
      },
      proxy: {
        title: 'Proxy',
        description:
          'Comunicado pelo proxy da organização. O conteúdo das configurações e os registos permanecem privados.',
        lastHeartbeat: 'Último sinal de atividade',
        never: 'Ainda não há sinal de atividade',
        runningVersion: 'Versão da configuração em execução',
        latestVersion: 'Versão mais recente da configuração',
        notAvailable: 'Não disponível',
        configStatus: 'Estado da configuração',
        activeBans: 'Banimentos ativos',
      },
      members: {
        title: 'Membros',
        description: 'Apenas nomes e funções.',
        caption: 'Membros da organização',
        name: 'Membro',
        role: 'Função',
        empty: 'Esta organização não tem membros.',
      },
    },
    audit: {
      meta: {
        title: 'Auditoria — Proxyma',
      },
      title: 'Auditoria',
      description:
        'Atividade de administração da plataforma, mais recente primeiro. Esta vista é só de leitura.',
      from: 'De',
      to: 'Até (exclusivo)',
      action: 'Ação',
      allActions: 'Todas as ações',
      timeHelp:
        'As horas são apresentadas em {timezone}. O início é inclusivo e o fim é exclusivo. Os filtros são mantidos no URL.',
      invalidRange: 'O fim do intervalo tem de ser posterior ao início.',
      tableCaption: 'Eventos de auditoria da plataforma',
      columns: {
        time: 'Hora',
        actor: 'Autor',
        action: 'Ação',
        target: 'Alvo',
        summary: 'Resumo',
        sourceIp: 'IP de origem',
        details: 'Detalhes',
      },
      actions: {
        user: {
          view: 'Consultou o utilizador',
          suspend: 'Suspendeu o utilizador',
          unsuspend: 'Retirou a suspensão ao utilizador',
          force_logout: 'Forçou a terminação de sessão',
        },
        org: {
          view: 'Consultou a organização',
        },
        platform_admin: {
          grant: 'Concedeu administração da plataforma',
          revoke: 'Revogou administração da plataforma',
        },
      },
      systemActor: 'Sistema',
      noTarget: 'Sem alvo',
      noSourceIp: 'Não registado',
      details: 'Detalhes',
      detailsTitle: 'Evento de auditoria',
      detailsDescription: 'Detalhes só de leitura desta ação de administração da plataforma.',
      detailsFields: {
        time: 'Hora',
        actor: 'Autor',
        action: 'Código da ação',
        target: 'Alvo',
        summary: 'Resumo',
        sourceIp: 'IP de origem',
      },
      loaded: 'Eventos carregados: {count}.',
      empty: {
        title: 'Nenhum evento de auditoria neste intervalo',
        description: 'Experimente um intervalo de tempo mais amplo ou limpe o filtro de ação.',
      },
    },
    stepUp: {
      reason: 'Motivo',
      reasonHelp: 'Explique porquê. Máximo de 500 caracteres.',
      password: 'A sua palavra-passe',
      reasonRequired: 'Introduza um motivo.',
      passwordRequired: 'Introduza a sua palavra-passe.',
      warning: 'Esta ação fica registada na auditoria da plataforma.',
    },
  },
  errors: {
    INVALID_CREDENTIALS: 'O email ou a palavra-passe estão incorretos.',
    ACCOUNT_SUSPENDED:
      'Esta conta está suspensa. Contacte o operador se achar que se trata de um erro.',
    RATE_LIMITED: 'Demasiadas tentativas. Tente novamente dentro de {seconds} segundos.',
    EMAIL_TAKEN: 'Já existe uma conta com este email. Inicie sessão em vez disso.',
    WEAK_PASSWORD:
      'Escolha uma palavra-passe mais forte. Utilize pelo menos 10 caracteres e evite palavras-passe comuns.',
    VALIDATION_FAILED: 'Verifique os campos assinalados e tente novamente.',
    MALFORMED_REQUEST: 'Não foi possível ler esse pedido. Atualize a página e tente novamente.',
    INTERNAL_ERROR:
      'Ocorreu um erro da nossa parte. Tente novamente. Se o problema persistir, contacte o operador.',
    NETWORK_ERROR:
      'Não foi possível contactar a Proxyma. Verifique a sua ligação e tente novamente.',
    TIMEOUT: 'O pedido demorou demasiado tempo. Tente novamente.',
    CORS_REJECTED: 'Este pedido veio de uma origem que a Proxyma não permite.',
    FORBIDDEN: 'Não tem permissão para fazer isso.',
    UNAUTHORIZED: 'Inicie sessão para continuar.',
    INVALID_REFRESH_TOKEN: 'A sua sessão terminou. Inicie sessão novamente.',
    SESSION_REFRESH_DELAYED:
      'A sua sessão está a ser renovada. Tente novamente dentro de momentos.',
    NOT_FOUND: 'Esta página não existe ou não tem acesso.',
    INVALID_CURSOR: 'Esta página de resultados já não é válida. Aplique os filtros novamente.',
    INVALID_PARAMETER: 'Um dos filtros não é válido. Verifique-o e tente novamente.',
    CONFLICT:
      'Essa alteração entra em conflito com o estado atual. Atualize a página e tente novamente.',
    REAUTH_FAILED: 'Essa palavra-passe está incorreta. Tente novamente.',
    ADMIN_CANNOT_TARGET_ADMIN: 'Os administradores da plataforma não podem ser suspensos.',
    CANNOT_SUSPEND_SELF: 'Não pode suspender a sua própria conta.',
    UNKNOWN: 'Ocorreu um erro. Tente novamente.',
  },
  notFound: {
    meta: {
      title: 'Página não encontrada — Proxyma',
    },
    title: 'Página não encontrada',
    description: 'Esta página não existe ou não tem acesso.',
    back: 'Voltar à página inicial',
  },
}

export default ptPT

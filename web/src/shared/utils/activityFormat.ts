export interface ActivityLike {
  actorName?: string;
  action: string;
  targetType?: string;
  targetName?: string;
  repositoryName?: string;
  description?: string;
}

const ACTION_ALIASES: Record<string, string> = {
  UPLOADED_A_MATERIAL: 'UPLOADED_MATERIAL',
  CREATED_A_REPOSITORY: 'CREATED_REPOSITORY',
  UPDATED_A_REPOSITORY: 'UPDATED_REPOSITORY',
  INVITED_A_MEMBER: 'INVITED_MEMBER',
  JOINED_A_REPOSITORY: 'JOINED_REPOSITORY',
  LEFT_A_REPOSITORY: 'LEFT_REPOSITORY',
  REQUESTED_A_MATERIAL: 'CREATED_REQUEST',
  FULFILLED_A_REQUEST: 'FULFILLED_REQUEST',
  CLOSED_A_REQUEST: 'CLOSED_REQUEST',
  DELETED_A_MATERIAL: 'DELETED_MATERIAL',
};

export const getActivityActionKey = (action = '') => {
  const key = action.trim().toUpperCase().replace(/[^A-Z0-9]+/g, '_').replace(/^_|_$/g, '');
  return ACTION_ALIASES[key] || key;
};

const readableAction = (action = '') => {
  const words = action.trim().replace(/_/g, ' ').toLowerCase();
  return words || 'performed an action';
};

const withActor = (message: string, log: ActivityLike, includeActor: boolean, selfLabel: string, isCurrentUser?: boolean) => {
  if (!includeActor) return message;
  const actor = isCurrentUser ? 'You' : (log.actorName || selfLabel);
  return `${actor} ${message}`;
};

export const formatActivityMessage = (
  log: ActivityLike,
  options: { includeActor?: boolean; includeRepositoryName?: boolean; selfLabel?: string; isCurrentUser?: boolean } = {}
) => {
  const includeActor = options.includeActor ?? true;
  const includeRepositoryName = options.includeRepositoryName ?? true;
  const selfLabel = options.selfLabel || 'Someone';
  const actionKey = getActivityActionKey(log.action);
  const target = (log.targetName || '').trim();
  const repo = (log.repositoryName || '').trim();

  switch (actionKey) {
    case 'UPLOADED_MATERIAL':
      return withActor(`uploaded material "${target || 'Untitled material'}"`, log, includeActor, selfLabel, options.isCurrentUser);
    case 'CREATED_REPOSITORY':
      return withActor(`created repository "${target || repo || 'Untitled repository'}"`, log, includeActor, selfLabel, options.isCurrentUser);
    case 'UPDATED_REPOSITORY':
      return withActor(`updated repository "${target || repo || 'Untitled repository'}"`, log, includeActor, selfLabel, options.isCurrentUser);
    case 'INVITED_MEMBER':
      return withActor(`invited ${target || 'a member'}${includeRepositoryName && repo ? ` to "${repo}"` : ''}`, log, includeActor, selfLabel, options.isCurrentUser);
    case 'JOINED_REPOSITORY':
      return withActor(`joined repository "${repo || target || 'this repository'}"`, log, includeActor, selfLabel, options.isCurrentUser);
    case 'LEFT_REPOSITORY':
      return withActor(`left repository "${repo || target || 'this repository'}"`, log, includeActor, selfLabel, options.isCurrentUser);
    case 'CREATED_REQUEST':
      return withActor(`requested material "${target || 'Untitled request'}"`, log, includeActor, selfLabel, options.isCurrentUser);
    case 'FULFILLED_REQUEST':
      return withActor(`fulfilled request "${target || 'Untitled request'}"`, log, includeActor, selfLabel, options.isCurrentUser);
    case 'CLOSED_REQUEST':
      return withActor(`closed request "${target || 'Untitled request'}"`, log, includeActor, selfLabel, options.isCurrentUser);
    case 'DELETED_MATERIAL':
      return withActor(`deleted material "${target || 'Untitled material'}"`, log, includeActor, selfLabel, options.isCurrentUser);
    case 'POSTED_UPDATE':
      return withActor(`posted an update in "${repo || 'this repository'}"`, log, includeActor, selfLabel, options.isCurrentUser);
    default:
      return withActor(`${log.description || readableAction(log.action)}${target ? ` "${target}"` : ''}`, log, includeActor, selfLabel, options.isCurrentUser);
  }
};
